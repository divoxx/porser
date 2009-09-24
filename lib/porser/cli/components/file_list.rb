module Porser
  module CLI
    module Components
      class FileList
        def initialize(path, opts = {})
          @title       = opts.fetch(:title, "Available files")
          @question    = opts.fetch(:question, "Select a file")
          @only_folder = opts.fetch(:only_folder, false)
          @full_path   = opts.fetch(:full_path, true)
          @multiple    = opts.fetch(:multiple, false)
          @allow_none  = opts.fetch(:allow_none, false)
          @path        = path
        end
      
        def ask        
          files = Dir["#{@path}/*"]
          
          puts "#{@title}:" if @title
          
          if files.empty?
            puts "No file available!"
            exit(1)
          end
        
          if @allow_none
            puts "  [0] None"
          end
          
          files.each_with_index do |path, i|
            puts "  [#{"%1d" % [@allow_none ? i+1 : i]}] #{File.basename(path)}#{File.directory?(path) ? '/' : nil}" if !@only_folder || File.directory?(path)
          end
        
          print "#{@question}"
          print " (Multiple options allowed, separated by comma, or * for all)" if @multiple
          print ": "
          input = $stdin.gets.chomp
          puts ""
          
          if @multiple
            if input == "*"
              selected_idx = (0...files.size)
            else
              selected_idx = input.split(/,/).map { |token| Integer(token) }
            end
          else
            selected_idx = Array(Integer(input))
          end
          
          if @allow_none
            selected_idx.delete(0)
            selected_idx.map! { |i| i - 1 }
          end

          values = files.values_at(*selected_idx)
          values.map! { |v| File.basename(v).gsub(/\.[^\.]+$/, '') } unless @full_path
          @multiple ? values : values.first
        end
      end
    end
  end
end