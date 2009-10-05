module Porser
  module Filters
    class RemoveNounSubcategories
      def self.description
        "Remove todas as subcategorias dos substantivos, transformando N_* em simplesmente N"
      end
      
      def tag(tag)
        tag =~ /^N_/ ? "N" : tag
      end
    end
  end
end