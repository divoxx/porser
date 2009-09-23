module Porser
  module Filters
    class RemoveTagHyphen
      def tag(tag)
        tag.gsub(/(\w)-(\w)/, '\1\2')
      end
    end
  end
end