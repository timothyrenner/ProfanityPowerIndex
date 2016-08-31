# Profanity Power Index

This guide contains the information required to produce the graphic and collect the data for the [Profanity Power Index](http://timothyrenner.github.io/profanitypowerindex).

## Quickstart

To just get a look at a sample graphic, `cd` into `/site/sample_data` and run

```bash
python create_sample_debate_data.py > sample_data.tsv
```
This creates a tab separated file with simulated debate data.
You might also want to check out `sample_subjects.json` while you're there, but don't change anything because it needs to match the `tsv` data.

`cd` back up to root and go into `site_generator`.

From there run

```bash
lein run ../site/sample_data/all_config.json\
         sample_data/sample_data.tsv >\
         ../site/index.html
```

Here's what the args are, very quickly.
This will be discussed in more detail later in the document.

1. `../site/sample_data/all_config.json` is a configuration file that gives information the templating engine needs about the subjects like: picture URL, color schemes, and CSS id tags.

2. `sample_data/sample_data.tsv` is the path *from the `index.html` file* to the data. It's not going to be used as a path in the templating engine, just the site's HTML. That's why you **don't** need `../site/`. Yes this is confusing. If I come up with a better way I'll implement it.

3. `../site/index.html` is the path to the index file you want. The templater prints to STDOUT so it needs to be redirected.

Finally, `cd` into `site/` and fire up an HTTP server

```bash
python -m SimpleHTTPServer 8000
```

In your browser go to `http://localhost:8000` and enjoy.

## Building

The data collector is built with sbt, the post processor doesn't require building, and the site builder is built with Leiningen.

The sbt configuration is a little more complicated.
To build the Spark collector, use `sbt assembly` to make a fat jar.
That can be used for `spark-submit`.
For the local collectors, you can do `sbt "run OPTIONS"` and select the main class, or use `sbt stage` to build a launcher script to start the program without sbt.
The launcher script uses the [sbt-native-packager](http://www.scala-sbt.org/sbt-native-packager/) plugin, with the default main class set to the InfluxDB collector.
The script has a `-main` option to select the STDOUT collector if you want to run that instead.

The site builder is explained later, but it's much simpler to run than the sbt stuff.

## Tweet Collector (Local Mode, STDOUT)

I built the local mode initially for testing, as it shares the same core logic as the distributed Spark Streaming build, but it could also be useful in case there are [issues running the cluster](http://timothyrenner.github.io/streaming/2015/08/16/adventures-in-the-mesosphere.html) or you just don't want to spend money on this blatantly stupid waste of time.
It takes five command line arguments:
    
* A configuration file (JSON).
* The Twitter consumer key.
* The Twitter consumer secret key.
* The Twitter access key.
* The Twitter access secret key.
    
It may seem like a bit much when running locally, but it's done this way because it makes the distributed mode more secure. See [here](http://timothyrenner.github.io/streaming/2015/08/16/adventures-in-the-mesosphere.html) for how I managed to screw this up with my AWS keys.
That was not a good morning.

Feel free to modify it to use a `twitter4j.properties` file if you want, just be aware that if you do run it remotely your credentials are going with it.

### Config File
Here's an example of the structure of the config file:

```json
    {
        "tracking": ["jeb bush", "jebbush", "ben carson", "realbencarson"],
        "targets": {
            "bush": "Jeb Bush",
            "@jebbush": "Jeb Bush",
            "carson": "Ben Carson",
            "@realbencarson": "Ben Carson"
        },
        
        "time": 120
    }
```

#### Fields
* `tracking` is an array of strings that will be used as filter queries against the Streaming API. Ideally it should contain both the full name (space included) and the twitter handle of each target.
* `targets` is an object (map, really) that contains _tokens_ from within tweets to the full names of the targets. For `@mention`s this means the `@` needs to be there, since the tweet is tokenized on space only (a subject of future work, perhaps). The best way to use this is to include the handle and the _last name_ of the subject. Used in conjunction with `tracking`, this can ensure the proper attribution is made (e.g. making sure Katy Perry doesn't get counted for Rick Perry).
* `time`: _Optional_. The total time in seconds for the run. If this is omitted, the program will run indefinitely.

### Post Processor (Local, STDOUT)

The streaming data collector's don't aggregate the tweet events as they come in because time-dependent aggregation can be tricky for streaming data.
So to perform the aggregation some post processing is required.
This happens in two stages:

1. Jump start the dataset by filling in every time/word/subject slot with a count of zero. This makes the visualization interpolators behave when there isn't much activity (see [here ](http://timothyrenner.github.io/profanitypowerindex/20150916-cnn/#john-kasich-sparkline) for an example).
2. Aggregate the event data, and merge with the jump-started dataset.

### Jump Start
There's a python3 script in the `post_processing/` directory called `jumpstart.py`.
It reads the site generator's config file, grabbing the subjects and the start/end times, and builds out a dataset of all zeros, outputting to a csv.
The next section describes the site generator's config file.

The start/end times in that file represent the _actual_ times the event started. 
If you collect data before and after the event start (to show ramped-up interest or something), you can provide a lead time that gets slapped on before and after the event.

The script is used as follows:
    
    python3 jumpstart.py /path/to/site-generator-config.json [OPTIONS]
    
Options:
    
    -l --lead-time <time> Time in minutes to put at the beginning/end of the run. Default 15.
    
    -o --output <file> The name of the output file. Default "output.csv"
    
    -h --help Displays arguments
    
### Aggregation
The event aggregation is done via [SQLite3](https://www.sqlite.org/), but is fully automated in a bash script, `process_events.sh`.
This script also requires [csvkit](https://csvkit.readthedocs.org/en/0.9.1/) for some date picking magic.

It's usage is simple:

```bash
process_events.sh /path/to/raw_data.csv /path/to/jumpstart_data.csv
```

It outputs a tab-separated file with the time, subject, word, and count (including headers) in the exact form required for the visualization.
It even filters the raw data to confine it between the start and stop times in the jump started dataset.

The output goes to stdout, so you'll want to redirect it to the file of your choice.

## Tweet Collector (Spark)

The Spark collector for the tweets contains exactly the same command line options as the local mode.
Spark mode is designed to store the tweets (text, time) as well as the profanity into a Cassandra database.
It sets up the keyspace `ppi` and creates two tables: `tweets` and `profanity`.
The JSON config file requires three additional arguments (which are ignored by local mode):

* `batchLength`: The number of seconds per micro-batch.
* `cassandraHost`: The hostname of the Cassandra database.
* `cassandraPort`: The port of the Cassandra database.

Post processing is the same as for local STDOUT mode - pull down the `profanity` table into a tab separated file.

## Tweet Collector (Local, InfluxDB)

There's also an option to use [InfluxDB](https://influxdata.com/time-series-platform/influxdb/) to store the profanity in local mode.

It expects InfluxDB to be running on `localhost:8086`.
Future versions may make this configurable, but it's not hard to change in the code either.
It can be started (on my Mac) either in the background as a service, or simply in its own terminal, like so

```bash
influxd
```

### Post Processor (Local, InfluxDB)

The post processing step for the InfluxDB mode is simpler than the local mode since InfluxDB allows grouping by time much more flexibly than SQLite.
The post processor is located in `post_processing/influx`.

```bash
./extract_influx.sh start_time stop_time
```

where `start_time` and `stop_time` are the datetimes to grab the data in a format recognizable by the `gdate` (from GNU coreutils).
ISO 8601 or RFC3339 are good bets.
(Note: to install `gdate` on mac, use `brew install coreutils`).

The script converts the datetimes provided into UTC and puts them in the `WHERE` clause of an InfluxDB query that aggregates the profanity to the minute level, filling missing values with 0.
InfluxDB doesn't give the data in _quite_ the formate needed, so there's an `awk` script that the output gets piped to before being converted to tab separated values.
The script output is sent to STDOUT - capture it into a CSV to put it into the site.

## Site Generator

The site generator is a [leiningen](http://leiningen.org/) project (`brew install lein` on a mac) written in Clojure.
It uses the [hiccup](https://github.com/weavejester/hiccup) template engine to generate all of the HTML.

The site generator takes two arguments: the path to the subject file and the path to the dataset.


To run the site generator, cd into the `site_generator` directory and execute

```bash
lein run /path/to/subjects.json http/relative/path/to/data.tsv
```

Read on for descriptions of `subjects.json` and `data.tsv`.

### Site Generator Config File

The site generator config file is a JSON file with the following structure (with one map per subject):

```json
{
    "subjects":
        [
            { "name": "Rand Paul",
              "display_name": "Rand \"Filibuster\" Paul",
              "picture": "www.wikipedia.org",
              "id": "rand-paul",
              "colors": {
                 "sparkline": [{"offset": "xxx", "color": "xxx"}],
                 "barchart": { "base": "xxx", "hover": "xxx" }
            }
        ],
    "startTime": "YYYY-MM-DDTHH:MM-ZZZZ",
    "stopTime": "YYYY-MM-DDTHH:MM-ZZZZ"
}
```
#### Fields

* **name** The name of the subject.
* **picture** A link to a picture of the subject. This gets shoved into a 180px by 180px image tag. You can change it if you want, but it may have undesired consequences wrt the CSS positioning. Mind the aspect ratio as well.
* **id** A name that can be used as a CSS ID selector to uniquely identify DOM elements for a subject.
* **colors** The colors for the subject's plots. The **sparkline** is an array of objects that will be used in an [SVG gradient](https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Gradients). The idea was taken from [this plot](http://bl.ocks.org/mbostock/3969722) by Mike Bostock. The **barchart** is an object with the "base" color for the barchart bars and a color to transition to when hovered.
* **startTime** The start time of the actual debate in ISO 8601.
* **stopTime** The stop time of the actual debate in ISO 8601.

### Data File

A tab separated file with the following columns: subject, word, time, and count.
The time column should be in ISO 8601 or any other format recognizable by `new Date(time)` in javascript, since that's how it's interpreted.

### One Last Thing

The data file and the `generator-config.json` file need to match in that the names of the subjects in the dataset must match the names of the subjects in the file.
Otherwise things won't get drawn and stuff.

### Running

Run with leiningen

```bash
lein run /path/to/generator-config.json /index/to/data.tsv > ../site/index.html
```

## Site

The site itself with everything required to create the site, less the `index.html` file, which is built with the site generator program.
It contains the javascript file `setup.js`, which has all the plot functions required.
It also contains a directory with sample data - a sample `subjects.json` file and a `create_sample_data.py` python program to create the sample data.

### Creating Sample Data

The `create_sample_data.py` file builds some simulated sample data to load into the visualization.
Running it simply writes tab separated columns (subject, word, time, count) as tab separated values.
Redirect the output to save.

```bash
python create_sample_debate_data.py > sample_data.tsv
```

### Final Note
There are a couple of parameters that need to be changed within the site builder.
Be sure to look at the code. 
Specifically the start and stop times, and the link to my Github page.
Those are all in the `main` function.
