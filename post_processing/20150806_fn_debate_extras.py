import csv

candidates = ["Chris Christie", "Rand Paul", "Mike Huckabee",
              "Scott Walker", "Donald Trump", "Jeb Bush",
              "Ben Carson", "Ted Cruz", "Marco Rubio",
              "John Kasich"]
words = ["fuck", "shit", "ass", "bitch", "douche", "dick", "damn"]
times = ["2015-08-06T20:%02d-0400"%(i) for i in range(30,60)] + \
        ["2015-08-06T21:%02d-0400"%(i) for i in range(60)] + \
        ["2015-08-06T22:%02d-0400"%(i) for i in range(60)] + \
        ["2015-08-06T23:%02d-0400"%(i) for i in range(11)]

data = [{'subject': s, 'word': w, 'time': t, 'count': 0}
        for s in candidates
        for w in words
        for t in times]
        
with open("20150806_fn_debate_extras.csv","w") as out:
    fieldnames = ["subject", "word", "time", "count"]
    
    writer = csv.DictWriter(out, fieldnames=fieldnames, delimiter="\t")
    for row in data:
        writer.writerow(row)
    