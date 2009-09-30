module Porser
  module Filters
    class RemoveNounSubcategories
      def tag(tag)
        tag =~ /^N_/ ? "N" : tag
      end
    end
  end
end