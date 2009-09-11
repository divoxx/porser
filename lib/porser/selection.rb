module Porser
  class Selection
    class Mapper        
      def initialize(number_of_lines)
        @number_of_lines = number_of_lines
        @offset          = rand(number_of_lines)
        @mappings        = {}
        yield(self)
      end
    
      def map(percent, gold_io, parseable_io)
        raise ArgumentError, "percent must be >= 0 and <= 100" unless (0..100).include?(percent)
        size = (@number_of_lines * (percent / 100.0)).round
      
        if @offset + size - 1 > @number_of_lines
          ranges   = [@offset..@number_of_lines, 0..(@offset + size - @number_of_lines - 1)]
          @offset += size - @number_of_lines
        else
          ranges   = [@offset..(@offset + size - 1)]
          @offset += size
        end
              
        @mappings[ranges] = {:gold => gold_io, :parseable => parseable_io}
      end
    
      def run(n, line)
        @mappings.each do |ranges, ios|
          if ranges.any? { |r| r.include?(n) }
            line << "\n" unless line =~ /\n$/
            ios[:gold].write(line.squeeze(" "))
            ios[:parseable].write(line.gsub(/\([^\s]+|\)/, "").gsub(/^\s*(.*)\s*$/, "(\\1)\n").squeeze(" "))
          end
        end
      end
    end
    
    attr_reader :path
    BasePath = Porser.path.join('corpus', 'selections')
    
    def self.create!(corpus_path)
      selection = new(Time.now.utc.strftime(TimeFormat))
      Dir.mkdir(selection.path)
      
      begin
        ios = []
      
        number_of_lines = `cat #{corpus_path} | wc -l`.to_i
      
        mapper = Mapper.new(number_of_lines) do |m|
          {:train => 80, :dev => 10, :test => 10}.each do |what, percent|
            ios << gold_io = File.open(selection.gold_path_for(what), "w")
            ios << parseable_io = File.open(selection.parseable_path_for(what), "w")
            m.map percent, gold_io, parseable_io
          end
        end
              
        ios << corpus_file = File.open(corpus_path, "r")
        
        corpus_file.each_line do |line|
          mapper.run(corpus_file.lineno, line)
        end
      
      ensure
        ios.each { |io| io.close }
      end
      
      selection
    end
    
    def initialize(timestamp)
      @path = BasePath.join(timestamp)
    end
    
    def parseable_paths
      Dir["#{path}/*.parseable.txt"]
    end
    
    def gold_paths
      Dir["#{path}/*.gold.txt"]
    end
    
    def parseable_path_for(what)
      @path.join("#{what}.parseable.txt")
    end
    
    def gold_path_for(what)
      @path.join("#{what}.gold.txt")
    end
  end
end