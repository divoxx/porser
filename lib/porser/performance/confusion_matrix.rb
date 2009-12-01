module Porser
  module Performance
    class ConfusionMatrix
      include Enumerable
      
      def initialize
        @keys              = Set.new
        @mappings          = Hash.new { |h,k| h[k] = Hash.new { |h,k| h[k] = 0 }}
        @total_counter     = 0
        @correct_counter   = 0
        @error_counter     = 0
        @expected_counters = Hash.new { |h,k| h[k] = 0 }
      end
      
      def store(expected, got)
        @mappings[expected][got] += 1
        expected == got ? @correct_counter += 1 : @error_counter += 1
        @expected_counters[expected] += 1
        @total_counter += 1
        @keys << expected << got
      end
      
      def correctness
        @correct_counter.to_f / @total_counter.to_f
      end
      
      def errorness
        @error_counter.to_f / @total_counter.to_f
      end
      
      def get(expected, got)
        raise ArgumentError, "invalid key #{expected}" unless @keys.include?(expected)
        raise ArgumentError, "invalid key #{got}" unless @keys.include?(got)
        @mappings[expected][got]
      end
      
      def each
        sorted_keys = @keys.sort
        
        sorted_keys.each do |expected|
          sorted_keys.each do |got|
            if @expected_counters[expected] == 0
              yield(expected, got, 0.0)
            else
              yield(expected, got, @mappings[expected][got].to_f / @expected_counters[expected].to_f)
            end
          end
        end
      end
      
      def latex
        template = ERB.new(File.read(Porser.path.join('lib', 'templates', 'confusion_matrix.tex.erb')))
        template.result(binding)
      end
      
      def pretty_string
        sorted_keys = @keys.sort
        
        output = []
        output << (["%-10s"] * (sorted_keys.size + 1)).join(" | ") % ["TAG", *sorted_keys]
        
        sorted_keys.each do |key|
          output << ("%-10s | " + (["   %#.2f   "] * sorted_keys.size).join(" | ")) % [key, *sorted_keys.map { |k| @expected_counters[key] == 0 ? 0.0 : @mappings[key][k].to_f / @expected_counters[key].to_f }]
        end
        
        output.join("\n")
      end
    end
  end
end