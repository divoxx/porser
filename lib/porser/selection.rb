module Porser
  class Selection
    attr_reader :path
    
    BasePath   = Porser.path.join('corpus', 'selections')
    TimeFormat = "%Y%m%d%H%M%S"
    
    def self.create!(corpus_path)
      path = BasePath.join(Time.now.utc.strftime(TimeFormat))
      Dir.mkdir(path)
      new(path)
    end
    
    def self.find(timestamp)
      timestamp = timestamp.is_a?(Time) ? timestamp.strftime(TimeFormat) : timestamp.to_s
      path      = BasePath.join(timestamp)
      new(timestamp)
    end
    
    def initialize(path)
      @path = Pathname.new(path.to_s).check!
    end
    
    def parseable_paths
      Dir["#{path}/*.parseable.txt"]
    end
    
    def gold_paths
      Dir["#{path}/*.gold.txt"]
    end
    
    def train_parseable_path
      @path.join('train.parseable.txt')
    end
    
    def train_gold_path
      @path.join('train.gold.txt')
    end
    
    def devel_parseable_path
      @path.join('devel.parseable.txt')
    end
    
    def devel_gold_path
      @path.join('devel.gold.txt')
    end
    
    def test_parseable_path
      @path.join('test.parseable.txt')
    end
    
    def test_gold_path
      @path.join('test.gold.txt')
    end
    
    def train_log_path
      @path.join('train.log')
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