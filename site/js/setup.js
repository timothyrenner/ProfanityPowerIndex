// Reducers.

// Rolls a dataset by word, returning
// { word: String, count: Integer }
// as a map. This is used in conjunction with wordCounts.
function countByWord(a, x) {

    if(x.word in a) {
        a[x.word] += x.count;
    } else {
        a[x.word] = x.count;
    }
    
    return a;
}// Close countByWord.

// Rolls up the dataset by time, returning
// [ { time: Date object, count: Integer, ... } ]
//
// It also converts the date strings (ISO) into Date objects.
function countByTime(a, x) {

    if(a[a.length-1].time.getTime() === x.time.getTime()) {
        a[a.length-1].count += x.count;
    } else {
        a.push({count: x.count, time: x.time});
    }

    return a;
}// Close countByTime

//Counts words, returning as an array of objects.
// Takes a dataset [ { word: String, count: Integer, ...} ]
// and rolls into 
// [ { word: String, count: Integer } ], marginalizing other dimensions.
function wordCounts(d) {
    
    var c = [];
    var countDict = d.reduce(countByWord, {});
    
    for(var w in countDict) {
        c.push({'word': w, 'count': countDict[w]});
    }

    return c;
}// Close wordCounts.

// Filters all entries matching a specific candidate.
// data looks like [ { candidate: String , .... } ]
function filterCandidate(c, d) {

    return d.filter(function(x) { return x.candidate === c; });
}

// Filters all entries matching a specific word.
// data looks like [ { word: String, ... } ]
function filterWord(w, d) {

    return d.filter(function(x) { return x.word === w; });
}

// Returns the width of the specified div.
function elementWidth(divId) {
    return parseInt(window.getComputedStyle(document.getElementById(divId))
                          .width) - 5;
}// Close elementHeight.

// D3 functions.
// data: [{'candidate' : candidate,
//         'word'      : word,
//         'time'      : time,
//         'count'     : count }]

// TODO: Add x-axes to both charts?
// TODO: Fill the actual debate times on the sparklines?
function sparkline(d3, id, data, width, height, gradient) {

    var dataByTime = data.slice(1).reduce(countByTime,
        [
            {
                time: data[0].time,
                count: data[0].count
            }]);

    // Offsets for endpoints.
    var widthOffset = 6;
    var heightOffset = 6;

    // Perform the initial selection.
    var svg = d3.select("#"+id).append("svg")
                           .attr("id", id + "-svg")
                           .attr("height", height)
                           .attr("width", width);

    // Create the scales.
    var xScale = d3.time.scale()
                        .range([widthOffset, width - widthOffset])
                        .domain(d3.extent(dataByTime, 
                                          function(d) { return d.time; }));
    
    var yScale = d3.scale.linear()
                          .range([height - heightOffset, heightOffset])
                          .domain(d3.extent(dataByTime, 
                                            function(d) { return d.count; }));

    // Create the line function.
    var line = d3.svg.line()
                     .x(function(d) { return xScale(d.time); })
                     .y(function(d) { return yScale(d.count); })
                     .interpolate("basis");

    // Grab the max counts.
    var maxCount = d3.max(dataByTime, function(d) { return d.count; });

    // Create the color gradient.
    svg.append("linearGradient")
       .attr("id", id + "-svg-count-gradient")
       .attr("gradientUnits", "userSpaceOnUse")
       .attr("x1", xScale(0)).attr("y1", yScale(0.10*maxCount))
       .attr("x2", xScale(0)).attr("y2", yScale(0.90*maxCount))
       .selectAll("stop")
       .data(gradient)
       .enter()
       .append('stop')
       .attr("offset", function(d) { return d.offset; })
       .attr("stop-color", function(d) { return d.color; });

    // Add the line to the svg.
    svg.append("path")
       .data([dataByTime])
       .attr("id", id + "-line")
       .attr("class", "line")
       .attr("d", line)
       .attr("stroke-width", 2.5)
       .attr("fill", "none")
       .attr("stroke", "url(#"+id+"-svg-count-gradient)");

    // Add the end points to the svg.
    svg.selectAll("circle")
       .attr("class", "end-points")
       .data([dataByTime[0], dataByTime[dataByTime.length-1]])
       .enter()
       .append("circle")
       .attr("r", 5)
       .attr("cx", function(d) { return xScale(d.time); })
       .attr("cy", function(d) { return yScale(d.count); })
       .attr("fill", "url(#"+id+"-svg-count-gradient)");
} // Close sparkline.

function updateSparkline(d3, id, data, width, height) {

    // Slice out the data by time.
    var dataByTime = data.slice(1).reduce(countByTime,
        [
            {
                time: data[0].time,
                count: data[0].count
            }]);

    // Offsets for endpoints.
    var widthOffset = 6;
    var heightOffset = 6;

    // Redraw the y-scale.
    var yScale = d3.scale.linear()
                          .range([height - heightOffset, heightOffset])
                          .domain(d3.extent(dataByTime, 
                                            function(d) { return d.count; }));
    var xScale = d3.time.scale()
                        .range([widthOffset, width - widthOffset])
                        .domain(d3.extent(dataByTime, 
                                          function(d) { return d.time; }));
    // Create the line new line function.
    var line = d3.svg.line()
                     .x(function(d) { return xScale(d.time); })
                     .y(function(d) { return yScale(d.count); })
                     .interpolate("basis");

    var svg = d3.select("#"+id+"-svg");
    var transitionTime = 500;

    // Update the line for the svg.
    svg.selectAll("path")
      .data([dataByTime])
      .transition()
      .duration(transitionTime)
      .attr("d", line);

    // Add the end points to the svg.
    svg.selectAll("circle")
      .data([dataByTime[0], dataByTime[dataByTime.length-1]])
      .transition()
      .duration(transitionTime)
      .attr("cy", function(d) {return yScale(d.count);})
      .attr("fill", "url(#"+id+"-svg-count-gradient)");
} // Close updateSparkline.

function hbar(d3, id, data, width, height, maxVal, color) {

    var countData = wordCounts(data);

    // Create the svg group.
    var svg = d3.select("#"+id)
                .append("svg")
                .attr("id", id + "-svg")
                .attr("width", width)
                .attr("height", height)
                .append("g")
                .attr("transform", "translate(" + -60 + ",0)")
                .attr("font-weight", "bold");

    // Create the x and y scales.

    // We need to anchor left, so the highest values need to
    // map to the smallest width so when we subtract the scale
    // value we get the remaining space to draw.
    // This is because SVG rect's draw from the top left.
    var xScale = d3.scale.linear()
                         .range([width, 60 + 5])
                         .domain([0, maxVal]);

    var yScale = d3.scale.ordinal()
                         .rangeRoundBands([0, height], 0.35)
                         .domain(data.map( function(d) { return d.word; }));

    // Draw the axis.
    var yAxis = d3.svg.axis().scale(yScale).orient("right");

    // Draw the bars.
    var bars = svg.selectAll(".bar")
       .data(countData)
       .enter()
       
    bars.append("rect")
       .attr("class","bar")
       .attr("x", function(d) { return xScale(d.count); })
       .attr("width", function(d) { return width - xScale(d.count); })
       .attr("y", function(d) { return yScale(d.word); })
       .attr("height", function(d) { return yScale.rangeBand(); })
       .attr("stroke", "black")
       .attr("stroke-width", "1.0px")
       .attr("fill", color.base)
       .on("mouseover", function(d) { 
            updateSparkline(d3, id.replace("barchart", "sparkline"), 
                filterWord(d.word, data), width, height); 
            updateBarChart(d3.select(this), color.hover);
            d3.selectAll("."+id+"-svg-text")
              .transition()
              .duration(500)
              .attr("fill-opacity","1")
              .attr("stroke-opacity","1");
            })
       .on("mouseout", function(d) { 
            updateSparkline(d3, id.replace("barchart", "sparkline"), data,
                width, height); 
            updateBarChart(d3.select(this), color.base);
            d3.selectAll("."+id+"-svg-text")
              .transition()
              .duration(500)
              .attr("fill-opacity", "0")
              .attr("stroke-opacity", "0");
            });

    // Draw the values "permanently".
    // May be able to use jquery to hide them.
    bars.append("text")
        .attr("class","bar-text")
        .attr("class", "noselect")
        .attr("class",id + "-svg-text")
        .attr("x", 
            function(d) { return ((d.count/maxVal) >= 0.1) ? 
                                 xScale(d.count) + 5 : xScale(d.count) - 5; })
        .attr("y", 
            function(d) { return yScale(d.word) + yScale.rangeBand()/1.35; })
        .text(function(d) { return d.count; })
        .attr("letter-spacing", "1")
        .attr("fill", 
            function(d) { 
                return ((d.count/maxVal) >= 0.1) ? "white": "black";})
        .attr("stroke","black")
        .attr("stroke-width", "0.5")
        .attr("text-anchor", 
            function(d) { 
                return ((d.count/maxVal) >= 0.1) ? "start": "end"; })
        .attr("fill-opacity","0")
        .attr("stroke-opacity", "0");
        
    // Draw the axis.       
    svg.append("g")
       .attr("class", "y-axis")
       .attr("transform","translate(" + width + ",0)")
       .call(yAxis)
       .select("path")
       .attr('fill','none')
       .attr('stroke', 'none');
}// Close hbar.

function updateBarChart(selection, color, count) {
    selection
      .transition()
      .duration(500)
      .attr("fill", color);

}// Close updateBarChart.

// Constants.

// Height of the plots.
var plotHeight = 229;

function getMaxValue(data) {
    // All counts per candidate, word.
    var allCounts = data.reduce(function(a, x) {
        if(x.candidate in a) {
            if(x.word in a[x.candidate]) {
                a[x.candidate][x.word] += x.count;
            } else {
                a[x.candidate][x.word] = x.count;
            }
        } else {
            a[x.candidate] = {};
            a[x.candidate][x.word] = x.count;
        }
        return a;
    }, {});

    // Maximum word, candidate combination.
    var maxValue = 
        Object.keys(allCounts)
              .reduce(function(a, x) { 
                  return Math.max(a, Object.keys(allCounts[x]).reduce( 
                      function(a2, x2) { 
                        return Math.max(a2, allCounts[x][x2]); }, 0))}, 0);

    return maxValue;
}// Close getMaxValue.

// Generates the callback for the d3.tsv function based on the contents of
// candidateString.
function tsvCallback(candidatesString) {

    var cb = function(data) {
        
        cleanedData = data.map(function(x) {
            // Convert date type and truncate to minute.
            var newTime = new Date(x.time);
            newTime.setSeconds(0);
            newTime.setMilliseconds(0);

            // Convert count to integer.
            return { 
                candidate: x.candidate,
                word: x.word,
                time: newTime,
                count: parseInt(x.count)
            };
        });

        var maxValue = getMaxValue(cleanedData);
        var candidates = candidatesString;

        // For each candidate, draw the sparkline and barchart.
        candidates.forEach(
            function(x) {
                sparkline(d3,
                          x.id + "-sparkline", 
                          filterCandidate(x.name, cleanedData),
                          elementWidth(x.id + "-sparkline"),
                          plotHeight,
                          x.colors.sparkline);
                hbar(d3,
                     x.id + "-barchart",
                     filterCandidate(x.name, cleanedData),
                     elementWidth(x.id + "-barchart"),
                     plotHeight,
                     maxValue,
                     x.colors.barchart);
            });
    };

    return cb;
} // Close tsvCallback.
