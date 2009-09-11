module Porser
  class Experiment
    attr_reader :path
    
    def self.create!(selection, filters = [])
      experiment = new(selection.join("#{Time.now.utc.strftime(TimeFormat)}-#{filters.join("-")}"))
      Dir.mkdir(experiment.path)
      
      # Apply filters on corpus
      
      experiment
    end
    
    def initialize(path)
      @path      = path
      @selection = Selection.new(File.dirname(@path))
      @filters   = File.basename(@path).split("-").map { |fiter_name| puts filter_name }
    end
    
    def train_parseable_path
      @path.join('corpus.train.parseable.txt')
    end
    
    def train_gold_path
      @path.join('corpus.train.gold.txt')
    end
    
    def devel_parseable_path
      @path.join('corpus.devel.parseable.txt')
    end
    
    def devel_gold_path
      @path.join('corpus.devel.gold.txt')
    end
    
    def test_parseable_path
      @path.join('corpus.test.parseable.txt')
    end
    
    def test_gold_path
      @path.join('corpus.test.gold.txt')
    end
    
    def head_rules_path
      @path.join('head-rules.lisp')
    end
    
    def score_path
      @path.join('score.txt')
    end
    
    def train_log_path
      @path.join('train.log')
    end
    
    def devel_parse_log_path
      @path.join('parse.devel.log')
    end
    
    def test_parse_log_path
      @path.join('parse.test.log')
    end
    
    def objects_path
      @path.join('objects.gz')
    end
    
    def observed_path
      @path.join('observed.gz')
    end
    
    def settings_path
      @path.join('settings.properties')
    end
    
    def head_rules_path
      @path.join('head-rules.list')
    end
  end
end