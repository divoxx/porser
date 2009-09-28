module Porser
  module Filters
    class RemoveConjSubcategories
      def tag(tag)
        tag =~ /^CONJ_/ ? "CONJ" : tag
      end
    end
  end
end