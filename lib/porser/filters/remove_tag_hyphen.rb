module Porser
  module Filters
    class RemoveTagHyphen
      def tag(tag)
        tag.gsub(/-/, '')
      end
    end
  end
end