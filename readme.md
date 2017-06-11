# Profanity Power Index

This guide contains the information required to produce the graphic and collect the data for the [Profanity Power Index](http://timothyrenner.github.io/profanitypowerindex).

## Quickstart

To just get a look at a sample graphic, `cd` into `dev-resources/` and run

```bash
python create_sample_debate_data.py
```
This creates a tab separated file with simulated debate data, `sample_data.csv`.
You might also want to check out `sample_config.json` while you're there, but don't change anything because it needs to match the `csv` data.

`cd` back up to root and run

From there run

```bash
lein run generate dev-resources/sample_config.json dev-resources/sample_data.csv
```

This creates a directory called `site`.
Here's what the args are, very quickly.
This will be discussed in more detail later in the document.

1. `dev-resources/sample_config.json` is a configuration file that gives information the templating engine needs about the subjects like: picture URL, color schemes, and CSS id tags.

2. `dev-resources/sample_data.csv` is the file with the data.

Finally, `cd` into `site/` and fire up an HTTP server

```bash
# You are using Python 3 .... right?
python -m http.server 8000
```

In your browser go to `http://localhost:8000` and enjoy.

## Overview

The Profanity Power Index is (now) a single Clojure program with three commands:

* `collect` Collects data from the Twitter API and streams it into Elasticsearch.
* `extract` Pulls data from Elasticsearch into a CSV file.
* `generate` Takes a CSV file and a JSON configuration and generates a website with an interactive D3.js visualization.

Run it one of two ways:

1. [Leiningen](https://github.com/technomancy/leiningen)

```
lein run <command> <OPTS>
```

2. Java

```bash
# To compile, first you have to tell Leiningen to build with a SNAPSHOT dependency.
# The only SNAPSHOT dependency is Turbine - read about it below.
export LEIN_SNAPSHOTS_IN_RELEASE=true
lein uberjar

# Now you can run it with java.
java -jar target/uberjar/profanity-power-index-2.0-standalone.jar <command> <OPTS>
```

Each command has its own set of options.
`collect` and `extract` also need some configuration for talking to Twitter and Elasticsearch.
The environment information needs to be stored in an [EDN](https://github.com/edn-format/edn) format (which basically means define a Clojure data structure, in our case a map).
Here's an example with the required keys, configured to talk to a locally running Elasticsearch instance.

```clojure
{
    :twitter-consumer-key "your-consumer-key"
    :twitter-consumer-secret "your-consumer-secret"
    :twitter-api-key "your-api-key"
    :twitter-api-secret "your-api-secret"
    :elasticsearch-url "http://localhost:9200"
    :elasticsearch-index "profanity_power_index"
}
```

You can specify this file with the `--env` argument, which defaults to `env.edn` (meaning it should be in the current directory you're running in).

### Installing Turbine

[Turbine](https://github.com/timothyrenner/turbine) is a data processing library I wrote, and is the only dependency **not** on Clojars / Maven Central.
To install it, clone the repo (linked) and run `lein install`.
Once turbine is feature complete I'll add it to Clojars, but it isn't there yet.
It does have enough functionality for the purposes of this application and works  well.

## `collect`

Assuming you have Twitter API keys, turbine installed, and a running Elasticsearch instance, this is a straightforward command to execute.
There is one option that can be repeated as necessary: `--track [-t]`, which specifies the tracking keyword in the Twitter [streaming API](https://dev.twitter.com/streaming/overview/request-parameters#track).

For example, to track President Trump, Vice President Pence, and Speaker Paul Ryan, with the environment configured in `env.edn`, you'd run:

```
lein run collect --track trump -t pence -t speakerryan
```

This would fire up a connection to Twitter, send the data for processing through turbine, which dumps it into Elasticsearch.
If you want to use Kibana, you can create some pretty cool dashboards while the data streams if you'd like, but that isn't required to get the data.

## `extract`

The `extract` subcommand is used to pull the data out of Elasticsearch into a CSV file for consumption by the site generator (or anything that can read a CSV file).
Here are the options:

```
--start-time -s START_TIME   The start time for the extraction.
--end-time   -e END_TIME     The end time for the extraction.
--target     -t QUERY TARGET The query string and associated target.
--output     -o OUTPUT_FILE  The name of the output file.
```

The times need to be in a very specific format: `YYYY-MM-DDTHH:mm:ssZ`, where Z is the time zone.
So June 9, 2017 at 10:30 PM CDT would be `2017-06-09T22:30:00-0500`.

The `--target` option takes two arguments: the query string and the target name.
The query string is used in a `match` query from Elasticsearch, and the target name is used for those matches in the resulting CSV file.
The match query is applied to two fields: `text` and `quoted.text`.
This is because Twitter's tracking command will return tweets with matches in the quoted text, even when there isn't a match in the text of the tweet itself.

So to match Donald Trump, Mike Pence, and Paul Ryan for tweets between 8:30 and 9:30 PI on Jun 1, 2017 Central Daylight Time, this is what you'd run:

```
lein run extract \
--start-time 2017-06-01T20:30:00-0500 \
--end-time 2017-06-01T21:30:00-0500 \
--target *trump* "Donald Trump" \
--target *pence* "Mike Pence" \
--target *ryan* "Paul Ryan" \
--output test.csv
```

Note the quotes around the target names.
If these aren't present the arguments will not be parsed correctly and the resulting CSV will be malformed.

The above command results in a file `test.csv` with the following tab separated fields: `subject` (defined by the second argument of the `-t` option), `word`, `time`, and `count`.
The file has a header.

## `generate`

The `generate` command takes a CSV (presumably created with the `extract` function) and a configuration file and generates a website with an interactive Javascript visualization.

It also takes an `--output-directory` or `-o` option to specify the directory to create the site in - default is ... `site`.
Because I'm creative.

If you've got a config file and data file and you want to make a site in `my_site`, this is what you'd execute.

```
lein run generate \
    my_config.json \
    my_data.csv \
    --output my_site
```

The application will create a directory `my_site` with a structure that looks like the following:

```
my_site/
    index.html
    js/profanitypowerindex.js
    data/my_data.csv
```

`cd` into that directory and fire up a web server and you have a fully functioning site with an interactive visualization.
Do note that the data file `my_site/data/my_data.csv` is _copied_ from the original.
The data file is the same format outputted by the `extract` command: a tab separated file with four fields - `subject`, `word`, `time`, and `count`. 
It also needs a header, or D3 won't be able to properly interpret the field names.

The config file is a little more complicated, and requires more explanation.

### generator Config File

The generator config file sets up parameters for the visualization on a target by target basis.

It has the following structure:

```javascript
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
            },
            {
                "name": "Donald Trump",
                // ... other fields for Trump.
            } ,
            // ... other people that were tracked.
        ],
    "startTime": "YYYY-MM-DDTHH:MM-ZZZZ",
    "stopTime": "YYYY-MM-DDTHH:MM-ZZZZ"
}
```

#### Fields

* **name** The name of the subject.
* **picture** A link to a picture of the subject. This gets shoved into a 180px by 180px image tag. You can change it if you want, but it may have undesired consequences wrt the CSS positioning. Mind the aspect ratio as well.
* **id** A name that can be used as a CSS ID selector to uniquely identify DOM elements for a subject. Hopefully it's obvious this has to be unique.
* **colors** The colors for the subject's plots. The **sparkline** is an array of objects that will be used in an [SVG gradient](https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Gradients). The idea was taken from [this plot](http://bl.ocks.org/mbostock/3969722) by Mike Bostock. The **barchart** is an object with the "base" color for the barchart bars and a color to transition to when hovered.
* **startTime** The start time of the actual debate in ISO 8601.
* **stopTime** The stop time of the actual debate in ISO 8601.

For a concrete example, a sample configuration is in `dev-resources/sample_config.json`.

### One Last Thing

The data file and the `generator-config.json` file need to match in that the names of the subjects in the dataset must match the names of the subjects in the file.
Otherwise things won't get drawn and stuff.

### Creating Sample Data

The `create_sample_data.py` file builds some simulated sample data to load into the visualization.
Running it simply writes tab separated columns (subject, word, time, count) as tab separated values to `sample_data.csv`.

```bash
python create_sample_debate_data.py
```
It requires python 3 to run.