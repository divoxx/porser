in_comment   = false
in_tag       = false
in_frase     = false
substr       = ""

while sentence = $stdin.read(4096)
  sentence.each_byte do |byte|
    if in_comment
      if byte == 10 then
        substr = ""
        in_comment = false
      end
      next
    end

    if byte == 32 && (substr[-1] == 32 || substr[-1] == 10)
      next
    else
      substr += (case byte when 10 then 32 when 45 then 95 else byte end).chr
    end

    case substr
    when /\A(.*)#\z/m then
      in_comment = true
      print $1
      substr = ""

    when /\A(.*)\(FRASE[^\(]+\(\z/m then
      print "#{$1}\n\(S "
      substr = "("
    
    when /\A(.*)\((?!FRASE)([^\s]+)\s\z/m then
      print "#{$1}("
      print $2.include?(":") ? $2.split(":")[1].upcase.gsub(/[^\w]+/, '') : $1
      print " "
      substr = ""

    when /\A(.*)\(([^\s]+)\)\z/m then
      print "#{$1}(#{$2} #{$2})"
      substr = ""

    when /\A(.*?)[\+_]?\)\z/m then
      print "#{$1})"
      substr = ""
    end
  end
end

print substr
