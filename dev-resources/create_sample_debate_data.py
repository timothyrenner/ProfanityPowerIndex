import csv
from random import randint
from datetime import datetime
from datetime import timedelta
from functools import reduce

candidates = ['Donald Trump', 'Paul Ryan', 'Mike Pence']

cuss_words = ['fuck', 'shit', 'ass', 'bitch', 
              'douche', 'dick', 'damn', 'covfefe']

# Three hours in `interval` second intervals.
interval = 60
times = [datetime.now() + timedelta(seconds=(interval*t)) 
         for t in range(int(60/interval) * 60 * 3)]

def random_walk(walk_length):
    return reduce(lambda a,x: a + [max(0, a[-1] + (-1)**randint(0,1)*x)],
                  [randint(0,3) for _ in range(walk_length)],
                  [randint(0,10)])

dataset = [{'subject': c, 'word': w, 'time': t.isoformat(), 'count': v}
            for c in candidates
            for w in cuss_words
            for t,v in zip(times, random_walk(len(times)))]

with open('sample_data.csv', 'w') as out:
    fieldnames = ["subject", "word", "time", "count"]

    writer = csv.DictWriter(out, fieldnames=fieldnames, delimiter="\t")
    
    writer.writeheader()
    for row in sorted(dataset, key=lambda x: x['time']):
        writer.writerow(row)
