module Porser
  module CLI
    class Question
      def initialize(question, opts = {})
        @default  = opts[:default]
        @answers  = opts[:answers] || {"y" => true, "n" => false}
        @input    = opts[:input] || $stdin
        @output   = opts[:output] || $stdout
        @question = question
      end
      
      def ask
        answers = case @default && @default.downcase 
          when "y" then "Y/n" 
          when "n" then "y/N"
          else
              "y/n"
        end
        
        @output.print "#{@question} [#{answers}] "
        answer      = @input.gets.chomp
        is_downcase = (answer.downcase == answer)
        default     = is_downcase && @default
        
        @answers[answer.downcase || default]
      end
    end
  end
end