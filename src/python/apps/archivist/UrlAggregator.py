import sys

sys.path.append('../../../mp/python/')
from ShMemObject import *
from ShMem import *
from ShMemAcquirer import *
from ShMemReleaser import *

class UrlAggregator:
    
    @staticmethod
    def get_urls(text):
        ret = []
        parts = text.split(' ')
        for p in parts:
            if p.startswith('HTTP://'):
                ret.append(p)

        return ret
    
    @staticmethod
    def process():
        next_tweet = 0
        while 1:
            
            # Get state from the acquirer. 
            ShMem.Acquire(0)            
            tweets = ShMem.s_state.get('tweets')
            vals = ShMem.s_state.get('url-aggregate')
            
            # Keep track of the total number of tweets. 
            num_tweets = len(tweets)
            while next_tweet != num_tweets:
                next_tweet_string = str(next_tweet)
                cur_tweet = tweets.get(next_tweet_string)

                # Get the tweet text and parse out the urls, if any. 
                tweet_text = cur_tweet['text'].upper()
                tweet_urls = UrlAggregator.get_urls(tweet_text)
                
                # Update the counts corresponding to each url. 
                for cur_url in tweet_urls:
                    if cur_url in vals.m_values:
                        cur_count = vals.get(cur_url)
                        vals.put_simple(cur_url, cur_count+1)

                    else:
                        vals.put_simple(cur_url, 1)

                next_tweet +=1
                        
            # Finally, send state back to the master node. 
            ShMem.Release(0)


def main():
    input_file = open(sys.argv[1])
    ShMem.Init(input_file, int(sys.argv[2]))
    input_file.close()
    ShMem.Start()
    UrlAggregator.process()


if __name__ == "__main__":
    main()
