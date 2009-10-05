module Porser
  module Filters
    class RemoveVerbSubcategories
      def self.description
        "Remove todas as subcategorias dos verbos, transformando V_* em simplesmente V"
      end
      
      def tag(tag)
        tag =~ /^V_/ ? "V" : tag
      end
    end
  end
end