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

function updateSparkline(d3, id, data) {
} // Close updateSparkline.