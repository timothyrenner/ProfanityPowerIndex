import json
import argparse
import sys
import csv
from datetime import datetime, timedelta

if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        "Creates zero-count data points for every minute of the debate.")
    parser.add_argument("config", 
        help="The name of the site generator configuration file.")
    parser.add_argument("-l", "--lead-time", default=15, type=int,
        help="""The number of minutes before and after the real debate 
                start/stop times.""")
    parser.add_argument("-o", "--output", default="output.csv",
        help="The name of the output file.")
    
    args = parser.parse_args()
    
    with open(args.config, 'r') as file:
        config = json.loads(file.read())
        
        candidates = [c['name'] for c in config['subjects']]
        words = ["fuck", "shit", "ass", "bitch", "douche", "dick", "damn"]
        
        
        ISO_format_string = "%Y-%m-%dT%H:%M%z"
        start_time = datetime.strptime(config['startTime'], ISO_format_string)\
            - timedelta(minutes=args.lead_time)
        stop_time = datetime.strptime(config['stopTime'], ISO_format_string)\
            + timedelta(minutes=args.lead_time)
        
        times = [(start_time + timedelta(minutes=m)).strftime(ISO_format_string)
                 for m in range((stop_time - start_time).seconds//60 + 1)]
        
        data = [{"subject": s, "word": w, "time": t, "count": 0}
                for s in candidates
                for w in words
                for t in times]
                
        with open(args.output, 'w') as out:
            fields = ["subject", "word", "time", "count"]
            writer = csv.DictWriter(out, fieldnames=fields, delimiter="\t")
            for row in data:
                writer.writerow(row)
    
    