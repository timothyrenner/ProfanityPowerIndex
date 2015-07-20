# Profanity Power Index

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
lein run ../site/sample_data/sample_subjects.json\
         sample_data/sample_data.tsv >\
         ../site/index.html
```

Here's what the args are, very quickly.
This will be discussed in more detail later in the document.

1. `../site/sample_data/sample_subjects.json` is a sort-of configuration file that gives information the templating engine needs about the subjects like: picture URL, color schemes, and CSS id tags.

2. `sample_data/sample_data.tsv` is the path *from the `index.html` file* to the data. It's not going to be used as a path in the templating engine, just the site's HTML. That's why you **don't** need `../site/`. Yes this is confusing. If I come up with a better way I'll implement it.

3. `../site/index.html` is the path to the index file you want. The templater prints to STDOUT so it needs to be redirected.

Finally, `cd` into `site/` and fire up an HTTP server

```bash
python -m SimpleHTTPServer 8000
```

In your browser go to `http://localhost:8000` and enjoy.

## Tweet Collector (Spark)

***TODO***: When it's implemented.

## Tweet Collector (Storm)

***TODO***: When it's implemented.

## Tweet Collector (Local Mode, Scala)

***TODO***: When it's implemented.

## Site Generator

The site generator is a [leiningen](http://leiningen.org/) project (`brew install lein` on a mac) written in Clojure.
It uses the [hiccup](https://github.com/weavejester/hiccup) to generate all of the HTML.

The site generator takes two arguments: the path to the subject file and the path to the dataset.


To run the site generator, cd into the `site_generator` directory and execute

```bash
lein run /path/to/subjects.json http/relative/path/to/data.tsv
```

Read on for descriptions of `subjects.json` and `data.tsv`.

### Subject File

The subject file is a JSON file with the following structure:

```
[
    { "name": "Rand Paul",
      "display_name": "Rand \"Filibuster\" Paul",
      "picture": "www.wikipedia.org",
      "id": "rand-paul",
      "colors": {
        "sparkline": [{"offset": "xxx", "color": "xxx"}, ...],
        "barchart": { "base": "xxx", "hover": "xxx" }
    }, ...
]
```
#### Fields

* **name** The name of the subject.
* **picture** A link to a picture of the subject. This gets shoved into a 180px by 229px image tag. You can change it if you want, but it may have undesired consequences wrt the CSS positioning.
* **id** A name that can be used as a CSS ID selector to uniquely identify DOM elements for a subject.
* **colors** The colors for the subject's plots. The **sparkline** is an array of objects that will be used in an [SVG gradient](https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Gradients). The idea was taken from [this plot](http://bl.ocks.org/mbostock/3969722) by Mike Bostock. The **barchart** is an object with the "base" color for the barchart bars and a color to transition to when hovered.

### Data File

A tab separated file with the following columns: subject, word, time, and count.
The time column should be in ISO 8601 or any other format recognizable by `new Date(time)` in javascript, since that's how it's interpreted.

### One Last Thing

The data file and the `subjects.json` file need to match in that the names of the subjects in the dataset must match the names of the subjects in the file.
Otherwise things won't get drawn and stuff.

### Running

Run with leiningen

```bash
lein run /path/to/subjects.json /index/to/data.tsv > ../site/index.html
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