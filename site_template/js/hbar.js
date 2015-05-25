function hbar(d3, id, data, width, height, maxVal) {
    
    var plot = {

        // Set up the SVG. We're leaving 60px slop on the right side for the
        // word "douche".
        graph: d3.select(id).append("svg:svg")
                 .attr("width", width)
                 .attr("height", height)
                 .append("g")
                 .attr("transform","translate(" + -60 + ",0)"),

        // Start the scales.
        x: d3.scale.linear()
                   // We need to anchor left, so the highest values need to
                   // map to the smallest width so when we subtract the axis
                   // value we get the remaining space to draw.
                   // This is because SVG rect's draw from the top left.
                   .range([width, 65])
                   .domain([0, maxVal]),

        y: d3.scale.ordinal()
                   .rangeRoundBands([0, height], 0.05)
                   .domain(data.map(function (x) { return x.lab; })),

        // We'll need to hang on to the data to make transition methods.
        data:data
    }; // Plot.


    // Next, create the axis.
    plot.yAxis = d3.svg.axis().scale(plot.y).orient("right");

    // Draws the bars.
    plot.graph.selectAll(".bar")
        .data(plot.data)
        .enter().append("rect")
        .attr("class","bar")
        .attr("x", function(d) { return plot.x(d.val); })
        .attr("width", function(d) { return width - plot.x(d.val); })
        .attr("y", function(d) { return plot.y(d.lab); })
        .attr("height", function(d) { return plot.y.rangeBand(); })
        .attr("fill", "blue");

    plot.graph.append("g")
              .attr("class", "y-axis")
              .attr("transform","translate(" + width + ",0)")
              .call(plot.yAxis)
              .select("path")
              .attr('fill','none')
              .attr('stroke', 'black');

    return plot; 
} // Close hbar.