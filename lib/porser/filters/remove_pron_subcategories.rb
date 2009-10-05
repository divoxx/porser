module Porser
  module Filters
    class RemovePronSubcategories
      def self.description
        "Remove todas as subcategorias dos pronomes, transformando PRON_* em simplesmente PRON"
      end
      
      def tag(tag)
        tag =~ /^PRON_/ ? "PRON" : tag
      end
    end
  end
end