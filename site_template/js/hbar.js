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