module Porser
  module Filters
    class RemovePronSubcategories
      def tag(tag)
        tag =~ /^PRON_/ ? "PRON" : tag
      end
    end
  end
end