module Porser
  module CLI
    module Components
      class FileList
        def initialize(path, opts = {})
          @title       = opts[:title] || "Available files"
          @question    = opts[:question] || "Select a file"
          @only_folder = opts[:only_folder] || false
          @path        = path
        end
      
        def ask        
          files = Dir["#{@path}/*"]

          puts "#{@title}:" if @title
        
          files.each_with_index do |path, i|
            puts "  [#{"%1d" % i}] #{File.basename(path)}#{File.directory?(path) ? '/' : nil}" if !@only_folder || File.directory?(path)
          end
        
          print "#{@question}: "
        
          selected_idx = Integer($stdin.gets) rescue ArgumentError
          files[selected_idx]
        end
      end
    end
  end
end