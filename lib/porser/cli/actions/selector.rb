require 'porser/selection'
require 'porser/cli/actions/base'

module Porser
  module CLI
    module Actions
      class Selector < Base
        NEW_LINE = 10
      
        class Mapper        
          def initialize(number_of_lines)
            @number_of_lines = number_of_lines
            @offset          = rand(number_of_lines)
            @mappings        = {}
          end
        
          def map(percent, &block)
            raise ArgumentError, "percent must be >= 0 and <= 100" unless (0..100).include?(percent)
            size = (@number_of_lines * (percent / 100.0)).round
          
            if @offset + size - 1 > @number_of_lines
              ranges   = [@offset..@number_of_lines, 0..(@offset + size - @number_of_lines - 1)]
              @offset += size - @number_of_lines
            else
              ranges   = [@offset..(@offset + size - 1)]
              @offset += size
            end
                  
            @mappings[ranges] = lambda { |what| block.call(what) }
          end
        
          def run(n, what)
            @mappings.each do |ranges, block|
              if ranges.any? { |r| r.include?(n) }
                block.call(what)
              end
            end
          end
        end
            
        def initialize(corpus_path, *args)
          @corpus_path = corpus_path
          super(*args)
        end
            
        def run!
          selection = Selection.create!(@corpus_path)
        
          train_gold_io      = file(selection.train_gold_path, "w")
          train_parseable_io = file(selection.train_parseable_path, "w")
          devel_gold_io      = file(selection.devel_gold_path, "w")
          devel_parseable_io = file(selection.devel_parseable_path, "w")
          test_gold_io       = file(selection.test_gold_path, "w")
          test_parseable_io  = file(selection.test_parseable_path, "w")
        
          mapper = Mapper.new(number_of_lines)
          
          mapper.map(80) do |what|
            train_gold_io.write(self.class.annot(what))
            train_parseable_io.write(self.class.clean(what))
          end
          
          mapper.map(10) do |what|
            devel_gold_io.write(self.class.annot(what))
            devel_parseable_io.write(self.class.clean(what))
          end
          
          mapper.map(10) do |what|
            test_gold_io.write(self.class.annot(what))
            test_parseable_io.write(self.class.clean(what))
          end
        
          info "Separating files into \"#{selection.path.to_s.gsub(/^#{Regexp.escape(Porser.path)}/, '')}\"\n"
        
          corpus_file.each_line do |line|
            mapper.run(corpus_file.lineno, line)
          end
        
          info "Done.\n"
        end
    
      protected
        def self.annot(line)
          unless line =~ /\n$/
            line << "\n"
          end
          line.squeeze(" ")
        end
      
        def self.clean(line)
          line.gsub(/\([^\s]+|\)/, "").gsub(/^\s*(.*)\s*$/, "(\\1)\n").squeeze(" ")
        end
        
        def corpus_file
          file(@corpus_path)
        end
      
        def number_of_lines
          unless @number_of_lines
            info "Calculating number of lines..."
            @number_of_lines = `cat #{@corpus_path} | wc -l`.to_i
            info "#{@number_of_lines}\n"
          end
          @number_of_lines
        end      
      end
    end
  end
end