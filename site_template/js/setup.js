// Reducers.
function timeReducer(a, x) {
    var xTime = Date.parse(x.time);
    
    if((a.length === 0) || (a[a.length-1].x != xTime)) {
        a.push({x: xTime, y: x.count});
    } else {
        a[a.length-1].y += x.count;
    }
    
    return a;
}// Close timeReducer.

function wordCountReducer(a, x) {
    
    if(x.word in a) {
        a[x.word] += x.count;
    } else {
        a[x.word] = x.count;
    }
    
    return a;
}// Close wordCountReducer.

function concatReducer(a, x) {

    return a.concat(x);
}// Close concatReducer.

//Counts words, returning as an array of objects.
function wordCounts(d) {
    
    var c = [];
    var countDict = d.reduce(wordCountReducer, {});
    
    for(var w in countDict) {
        c.push({'lab': w, 'val': countDict[w]});
    }

    c.sort(function(a, b) {
        if(a.lab < b.lab) {
            return -1;
        } else {
            return 1;
        }
    });

    return c;
}// Close wordCounts.

// Gets the word counts for a candidate.
// d: [{'time': time, 'candidate': candidate, 'count': count}]
function candidateWordCounts(c, d) {
    return wordCounts(d.filter(
        function(x) { return x.candidate === c; }));
}// Close candidateWordCounts.

// Gets the word counts for a candidate as a function of time.
// d: [{'time': time, 'candidate': candidate, 'count': count}]
function candidateWordCountsByTime(c, d) {

    return d.filter(function(x) { return x.candidate === c; })
            .reduce(timeReducer, []);
}// Close candidateWordCountsByTime.

// d: [{'word': word, 'count': count}]
function wordCountsByTime(w, d){

    return d.filter(function(x) { return x.word === w; });
}// Close wordCountsByTime.

// Returns the width of the selected div.
function elementWidth(divId) {
    return parseInt(window.getComputedStyle(document.getElementById(divId))
                          .width) - 5;
}// Close elementHeight.

// D3 functions.
function hbar(d3, id, data, width, height, maxVal, color) {
    // Create the svg group.
    var svg = d3.select(id)
                .append("svg:svg")
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
                         .domain(data.map( function(d) { return d.lab; }));

    // Draw the axis.
    var yAxis = d3.svg.axis().scale(yScale).orient("right");

    // Draw the bars.
    svg.selectAll(".bar")
       .data(data)
       .enter()
       .append("rect")
       .attr("class","bar")
       .attr("x", function(d) { return xScale(d.val); })
       .attr("width", function(d) { return width - xScale(d.val); })
       .attr("y", function(d) { return yScale(d.lab); })
       .attr("height", function(d) { return yScale.rangeBand(); })
       .attr("stroke", "black")
       .attr("stroke-width", "1.0px")
       .attr("fill", color);

    // Draw the axis.       
    svg.append("g")
       .attr("class", "y-axis")
       .attr("transform","translate(" + width + ",0)")
       .call(yAxis)
       .select("path")
       .attr('fill','none')
       .attr('stroke', 'none');
}// Close hbar.

function sparkline(d3, id, data, width, height, gradient) {

    // Offsets for endpoints.
    var widthOffset = 6;
    var heightOffset = 6;

    // Perform the initial selection.
    var svg = d3.select(id).append("svg:svg")
                           .attr("height", height)
                           .attr("width", width);

    // Create the scales.
    var xScale = d3.time.scale()
                        .range([widthOffset, width - widthOffset])
                        .domain(d3.extent(data, 
                                          function(d) { return d.x; }));
    
    var yScale = d3.scale.linear()
                          .range([height - heightOffset, heightOffset])
                          .domain(d3.extent(data, 
                                            function(d) { return d.y; }));

    // Create the line function.
    var line = d3.svg.line()
                     .x(function(d) { return xScale(d.x); })
                     .y(function(d) { return yScale(d.y); })
                     .interpolate("basis");

    // Grab the max counts.
    var maxCount = d3.max(data, function(d) { return d.y; });

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
    svg.append("svg:path")
       .data(data)
       .attr("d", line(data))
       .attr("stroke-width", 2.5)
       .attr("fill", "none")
       .attr("stroke", "url(#"+id+"-svg-count-gradient)");

    // Add the end points to the svg.
    svg.selectAll("circle")
       .data([data[0], data[data.length-1]])
       .enter()
       .append("circle")
       .attr("r", 5)
       .attr("cx", function(d) { return xScale(d.x); })
       .attr("cy", function(d) { return yScale(d.y); })
       .attr("fill", "url(#"+id+"-svg-count-gradient)");
} // Close sparkline.

