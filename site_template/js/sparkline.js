function sparkline(d3, id, data, width, height) {

    var plot = {
        graph: d3.select(id).append("svg:svg")
                            .attr("width", width)
                            .attr("height", height),

        x: d3.time.scale().range([0, width]),
        y: d3.scale.linear().range([height, 5]),
        data: data
    }; // Close plot.

    plot.x.domain(d3.extent(plot.data, function(d) { return d.x; }));
    plot.y.domain(d3.extent(plot.data, function(d) { return d.y; }));

    plot.line = d3.svg.line()
                      .x(function(d) { return plot.x(d.x); })
                      .y(function(d) { return plot.y(d.y); })
                      .interpolate("basis");

    plot.graph.append("svg:path")
        .data(plot.data)
        .attr("d", plot.line(plot.data))
        .attr("stroke-width", 2.5)
        .attr("fill","none");

    plot.color = function(c) {
        plot.graph.select("path").attr("stroke", c);
        return plot;
    }
    return plot;

}