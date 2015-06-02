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
}// Close wordCountsByTime