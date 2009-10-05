module Porser
  module Filters
    class RemoveConjSubcategories
      def self.description
        "Remove todas as subcategorias das conjunções, transformando CONJ_* em simplesmente CONJ"
      end
      
      def tag(tag)
        tag =~ /^CONJ_/ ? "CONJ" : tag
      end
    end
  end
end