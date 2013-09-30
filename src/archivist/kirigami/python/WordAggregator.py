

class WordAggregator:

    s_words = ['rt', 'about', 'after', 'again', 'air', 'all', 'along', 
               'also', 'an', 'and', 'another', 'any', 'are', 'around', 
               'as', 'at', 'away', 'back', 'be', 'because', 'been', 'before', 
    	       'below', 'between', 'both', 'but', 'by', 'came', 'can', 'come',
               'could', 'day', 'did', 'different', 'do', 'does', 'don\'t', 
               'down', 'each', 'end', 'even', 'every', 'few', 'find', 'first', 
               'for', 'found', 'from', 'get', 'give', 'go', 'good', 'great', 
               'had', 'has', 'have', 'he', 'help', 'her', 'here', 'him', 
               'his', 'home', 'house', 'how', 'I', 'if', 'in', 'into', 'is', 
               'it', 'its', 'just', 'know', 'large', 'last', 'left', 'like', 
               'line', 'little', 'long', 'look', 'made', 'make', 'man', 
               'many', 'may', 'me', 'men', 'might', 'more', 'most', 'Mr.', 
               'must', 'my', 'name', 'never', 'new', 'next', 'no', 
               'not', 'now', 'number', 'of', 'off', 'old', 'on', 'one', 
               'only', 'or', 'other', 'our', 'out', 'over', 'own', 'part', 
               'people', 'place', 'put', 'read', 'right', 'said', 'same', 
               'saw', 'say', 'see', 'she', 'should', 'show', 'small', 'so', 
               'some', 'something', 'sound', 'still', 'such', 'take', 'tell', 
               'than', 'that', 'the', 'them', 'then', 'there', 'these', 
               'they', 'thing', 'think', 'this', 'those', 'thought', 'three', 
               'through', 'time', 'to', 'together', 'too', 'two', 'under', 
               'up', 'us', 'use', 'very', 'want', 'water', 'way', 'we', 
               'well', 'went', 'were', 'what', 'when', 'where', 'which', 
               'while', 'who', 'why', 'will', 'with', 'word', 'work', 
               'world', 'would', 'write', 'year', 'you', 'your', 'was']

    @staticmethod
    def get_excludes(search_term):
        exclude_words = []
        for word in s_words:
            exclude_words.append(word.toUpper())        
        search_parts = search_term.split('\\s|\\+')
        for part in search_parts:
            exclude_words.append(part.toUpper())
        exclude_words.append(search_term.toUpper())

        return exclude_words
    
    @staticmethod
    def process_tweet(container, tweet_text, exclude_words): 
        tweet_text = tweet_text.toUpper()
        tweet_text = tweet_text.replace('#', '')
        tweet_text = tweet_text.replace('.', '')
        tweet_text = tweet_text.replace(',', '')
        
        tweet_peices = tweet_text.split(' ')
        for piece in tweet_pieces:
            if (len(piece) > 1 and 
            not (piece in exclude_words) and
            not (piece.startsWith('@')) and
            not (piece.startsWith('HTTP://'))):
                if piece in container.m_values:
                    val = container.get(piece)
                    container.put(piece, val+1)
                else:
                    container.put(piece)
        

    @staticmethod
    def process():
        next_tweet = 0
        next_tweet_string = ''
        exclude_words = None
        
        while 1:
            ShMem.Acquire(0)

            search_term = ShMem.s_state.get('search_term')
            tweets = ShMem.s_state.get('tweets')
            vals = ShMem.s_state.get('word-aggregate')
            
            if exclude_words == None:
                exclude_words = get_excludes(search_term)

            num_tweets = len(tweets)
            while next_tweet != num_tweets:
                next_tweet_string = str(next_tweet)
                tweet = tweets.get(next_tweet_string)
                tweet_text = tweet.get('text')
                process_tweet(vals, tweet_text, exclude_words)
                next_tweet += 1
            
            ShMem.Release(0)
            
def main():
    input_file = sys.argv[1]
    ShMem.Init(input_file, int(sys.argv[2]))
    input_file.close()
    ShMem.Start()
    WordAggregator.process()
                
                     
                 
            
