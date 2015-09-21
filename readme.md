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

## Tweet Collector (Local Mode, Scala)

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

## Tweet Collector (Spark)

The Spark collector for the tweets contains exactly the same command line options as the local mode.
The JSON config file requires two additional arguments (which are ignored by local mode):

* `batchLength`: The number of seconds per micro-batch.
* `filePrefix`: The prefix for the text file output (See the [Spark Streaming](http://spark.apache.org/docs/latest/streaming-programming-guide.html#output-operations-on-dstreams) documentation).

## Tweet Collector (Storm)

***TODO***: When it's implemented.

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
