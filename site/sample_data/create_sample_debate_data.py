import csv
from random import randint
from datetime import datetime
from datetime import timedelta

candidates = ['Ted Cruz', 'Rand Paul', 'Marco Rubio', 'Ben Carson', 
              'Mike Huckabee', 'Jeb Bush', 'Rick Perry', 'Rick Santorum',
              'Donald Trump']

cuss_words = ['fuck', 'shit', 'ass', 'bitch', 'douche', 'dick', 'damn']

# Three hours in `interval` second intervals.
interval = 60
times = [datetime.now() + timedelta(seconds=(interval*t)) 
         for t in range((60/interval) * 60 * 3)]

def random_walk(walk_length):
    return reduce(lambda a,x: a + [max(0, a[-1] + (-1)**randint(0,1)*x)],
                  [randint(0,3) for _ in range(walk_length)],
                  [randint(0,10)])

dataset = [{'candidate': c, 'word': w, 'time': t.isoformat(), 'count': v}
            for c in candidates
            for w in cuss_words
            for t,v in zip(times, random_walk(len(times)))]

with open('sample_data.csv', 'w') as out:
    fieldnames = ["candidate", "word", "time", "count"]

    writer = csv.DictWriter(out, fieldnames=fieldnames, delimiter="\t")
    
    writer.writeheader()
    for row in sorted(dataset, key=lambda x: x['time']):
        writer.writerow(row)
