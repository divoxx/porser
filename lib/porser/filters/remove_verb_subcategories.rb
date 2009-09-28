module Porser
  module Filters
    class RemoveVerbSubcategories
      def tag(tag)
        tag =~ /^V_/ ? "V" : tag
      end
    end
  end
end