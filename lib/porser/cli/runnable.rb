module Porser
  module CLI
    class Runnable
      def self.run(*args)
        instance = new(*args)
        instance.run!
      ensure
        instance.close_files!
      end
      
      def initialize(opts = {})
        @ios  = {}
        @opts = opts.freeze
      end
      
      def info(str)
        STDERR.print(str)
        STDERR.flush
      end
      
      def file(file_name, mode = "r")
        unless @ios[file_name]
          @ios[file_name] = File.open(file_name.to_s, mode)
        end
        @ios[file_name]
      end
      
      def close_files!
        @ios.values.each { |file| file.close }
      end
    end
  end
end