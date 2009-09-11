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
      @path      = Pathname.new(path.to_s).check!
      @selection = Selection.new(File.dirname(@path))
      @filters   = File.basename(@path).split("-").map { |fiter_name| puts filter_name }
    end
    
    def train!(heap_size = 700)
      cmd = "/usr/bin/env java"
      cmd << " -Xms#{heap_size}\\m -Xmx#{heap_size}\\m"
      cmd << " -cp \"#{Porser.java_classpath}:#{@path}\""
      cmd << " -Ddanbikel.parser.Model.printPrunedEvents=false"
      cmd << " -Dparser.settingsDir=\"#{@path}\""
      cmd << " -Dparser.settingsFile=\"#{settings_path.check!}\""
      cmd << " danbikel.parser.Trainer"
      cmd << " -i #{gold_path_for(:train).check!} -o #{observed_path} -od #{objects_path}"
      cmd << " > #{log_path_for(:train)} 2>&1"
      `#{cmd}`
    end
    
    def parseable_path_for(what)
      @path.join("corpus.#{what}.parseable.txt")
    end
    
    def gold_path_for(what)
      @path.join("corpus.#{what}.gold.txt")
    end
    
    def log_path_for(what)
      @path.join("log.#{what}.txt")
    end
    
    def head_rules_path
      @path.join('head-rules.lisp')
    end
    
    def score_path
      @path.join('score.txt')
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
  end
end