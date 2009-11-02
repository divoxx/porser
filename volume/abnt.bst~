%===============================================================================
% Estilo de referencia bibliografica da ABNT (documentos em portugues)
%
% Versao 1.0 - 20/jan/98
% Versao 2.0 - 13/dez/98	
%===============================================================================

%===============================================================================
% Definicao dos campos das entradas
%===============================================================================
ENTRY
{ 
  author
  editor
  title
  booktitle
  chapter
  pages
  address
  publisher
  year
  journal
  month
  number
  edition
  howpublished
  institution
  key
  note
  organization
  school
  series
  type
  volume
  totalpages
  numedition
  eventedition
  completion
}
{}
{
  label
}

INTEGERS {i n}
STRINGS {s last.label current.letter names new.pages longest.label}

%===============================================================================
% Definicao de strings usados na elaboracao das referencias
% Estes strings dependem do idioma no qual o documento foi escrito
%===============================================================================
FUNCTION {str:availableby} {"Dispon{\'\i}vel por "}
FUNCTION {str:at} {" em "}
FUNCTION {str:chapter} {"Cap."}
FUNCTION {str:mastersthesis} {"Disserta{\c{c}}{\~a}o de Mestrado"}
FUNCTION {str:phdthesis} {"Tese de Doutorado"}


%===============================================================================
% Funcoes logicas
%===============================================================================
FUNCTION {not}
{
    { #0 }
    { #1 }
  if$
}

FUNCTION {and}
{
    'skip$
    { pop$ #0 }
  if$
}

FUNCTION {or}
{
    { pop$ #1 }
    'skip$
  if$
}

%===============================================================================
% Escreve no arquivo o comando que gera o item
%
% Parm:
%   um string com a formatacao do item
%===============================================================================
FUNCTION {write.item}
{
  "\bibitem[" label * "]{" * cite$ * "} " *
  swap$ *
  write$ newline$ newline$
}

%===============================================================================
% Separa o titulo em titulo e sub-titulo, empilhando o sub-titulo e o titulo,
% nesta ordem
%
% Parm:
%   o titulo
%===============================================================================
FUNCTION {extract.subtitle}
{
  #1 'i :=

  duplicate$
  i #1 substring$
  {
    duplicate$ ":" = not
    swap$ "" = not
    and
  }
  {
    #1 i + 'i :=
    duplicate$
    i #1 substring$
  } while$

  duplicate$ i #999 substring$
  swap$
  #1 i #1 - substring$
}

%===============================================================================
% Separa a primeira palavra do texto, empilhando o resto e a palavra, nesta
% ordem
%
% Parm:
%   o texto
%===============================================================================
FUNCTION {extract.first.word}
{
  #1 'i :=

  duplicate$
  i #1 substring$
  {
    duplicate$ " " = not
    swap$ "" = not
    and
  }
  {
    #1 i + 'i :=
    duplicate$
    i #1 substring$
  } while$

  duplicate$ i #999 substring$
  swap$
  #1 i #1 - substring$
}

%===============================================================================
% Enfatiza um string
%
% Parm:
%   o string
%===============================================================================
FUNCTION {emphasize}
{
  "\emph{ " swap$ * "}" *
}

FUNCTION {emphasize2}
{
  "{\bf " swap$ * "}" *
}


%===============================================================================
% Adiciona um texto (se nao-empty$) ja' formatado ao item, separando-o por
% ponto.
% Ex.: NOME. Titulo. data. ano.
%
% Parm:
%   um string representando o item (pode ser vazio, para iniciar o item)
%   o texto a ser adicionado
%===============================================================================
FUNCTION {add.to.item}
{
  duplicate$ empty$
    'pop$
    {
      swap$
      duplicate$ empty$
        'skip$
        {" " *}
      if$
      swap$ add.period$ *
    }
  if$
}

%===============================================================================
% Adiciona um item (se nao-empty$) a uma lista do tipo "a, b, c, ..."
%
% Parm:
%   um string representando a lista (pode ser vazio, para criar uma lista)
%   o item a ser adicionado
%===============================================================================
FUNCTION {add.to.list}
{
  duplicate$ empty$
    'pop$
    {
      swap$
      duplicate$ empty$
        'skip$
        {", " *}
      if$
      swap$ *
    }
  if$
}

%===============================================================================
% Adiciona um nome a uma lista de nomes (separados por ponto-e-virgula)
%
% Parm:
%   um string representando a lista (pode ser vazio, para criar uma lista)
%   o nome a ser adicionado
%===============================================================================
FUNCTION {add.to.namelist}
{
  swap$
  duplicate$ empty$
    'skip$
    {"; " *}
  if$
  swap$ *
}

%===============================================================================
% Formata uma lista de nomes.
%
% Parm:
%   os nomes a serem formatados
%===============================================================================
FUNCTION {format.names}
{
  duplicate$ num.names$ 'n :=
  #1 'i :=
  "" 'names :=

  {i n #1 + <}
  {
    duplicate$ duplicate$ i "{ll}{ jj}" format.name$
    duplicate$ "others" =
      {pop$ pop$ names " et al." *}
      {
        swap$
        i "{, ff}{ vv}" format.name$ *
        names swap$ add.to.namelist
      }
    if$
    'names :=
    #1 i + 'i :=
  } while$
  pop$
  names
}

%===============================================================================
% Formata um titulo (pode ser `title' ou `booktitle')
%
% Parm:
%   um valor (0 ou 1) que indica se a formatacao deve ser enfatizada ou nao
%   um string com o titulo a ser formatado
%===============================================================================
FUNCTION {format.title}
{
  "t" change.case$
  swap$
    {extract.subtitle emphasize2}
    {extract.subtitle}
  if$
  swap$ "l" change.case$ *
}

%===============================================================================
% Retorna a indicacao de paginas ajustada para ter dois tracos (`--') se a
% entrada so' tiver um.
%===============================================================================
FUNCTION {get.adjusted.pages}
{
  #1 'i :=
  "" 'new.pages :=
  pages #1 #1 substring$
  {duplicate$ "" = not}
  {
    duplicate$ "-" =
    pages i #1 - #1 substring$ "-" = not
    and
    pages i #1 + #1 substring$ "-" = not
    and
      {"-" *}
      'skip$
    if$
    new.pages swap$ * 'new.pages :=
    #1 i + 'i :=
    pages i #1 substring$
  } while$
  pop$
  new.pages
}

%===============================================================================
% Coloca a primeira palavra do titulo em letras maiusculas e o resto em
% minusculas. Esta funcao e' usada para gerar referencias cuja entrada e' pelo
% titulo.
%===============================================================================
FUNCTION {format.entrytitle}
{
  #1 title format.title
  extract.first.word "u" change.case$
  swap$ * 
}

%===============================================================================
% Formatacao dos campos. Se o campo nao existir a funcao devolve um string
% vazio.
%===============================================================================
FUNCTION {format.author}
{
  author empty$
    {""}
    {author format.names}
  if$
}

FUNCTION {format.editor}
{
  editor empty$
    {""}
    {" (" editor format.names ",Eds.)" * *}
  if$
}

FUNCTION {format.edition}
{
  numedition empty$
    {""}
    {numedition "~ed." *}
  if$
}

FUNCTION {format.volume}
{
  volume empty$
    {""}
    {"v." volume *}
  if$
}

FUNCTION {format.number}
{
  number empty$
    {""}
    {"n." number *}
  if$
}

FUNCTION {format.month}
{
  month empty$
    {""}
    {month}
  if$
}

FUNCTION {format.address}
{
  address empty$
    {""}
    {address}
  if$
}

FUNCTION {format.publisher}
{
  publisher empty$
    {""}
    {publisher}
  if$
}

FUNCTION {format.year}
{
  year empty$
    {""}
    {year}
  if$
}

FUNCTION {format.chapter}
{
  chapter empty$
    {""}
    {
      type empty$
        {str:chapter}
        {type "~" *}
      if$
      chapter *
    }
  if$
}

FUNCTION {format.pages}
{
  pages empty$
    {""}
    {"p." get.adjusted.pages *}
  if$
}

FUNCTION {format.totalpages}
{
  totalpages empty$
    {""}
    {totalpages "p." *}
  if$
}

FUNCTION {format.note}
{
  note empty$
    {""}
    {"(" note * ")" *}
  if$
}

FUNCTION {format.month.year}
{
  month empty$
    {year}
    {month "~" * year *}
  if$
}

FUNCTION {format.type}
{
  type empty$
    'skip$
    {pop$ type}
  if$
}

%===============================================================================
% As seguintes funcoes adicionam um item a uma lista passada na pilha.
%===============================================================================
FUNCTION {add.address} {format.address add.to.list}
FUNCTION {add.publisher} {format.publisher add.to.list}
FUNCTION {add.editor} {format.editor add.to.list}
FUNCTION {add.year} {format.year add.to.list}
FUNCTION {add.chapter} {format.chapter add.to.list}
FUNCTION {add.edition} {format.edition add.to.list}
FUNCTION {add.volume} {format.volume add.to.list}
FUNCTION {add.number} {format.number add.to.list}
FUNCTION {add.month} {format.month add.to.list}
FUNCTION {add.totalpages} {format.totalpages add.to.list}
FUNCTION {add.pages} {format.pages add.to.list}
FUNCTION {add.note} {format.note add.to.list}
FUNCTION {add.month.year} {format.month.year add.to.list}

%===============================================================================
% Estas funcoes formatam partes comuns a varios tipos de referencia.
%===============================================================================
FUNCTION {format.publication}
{
  ""
  address empty$
    {
      ""
      publisher empty$
         {"" *}
         {" " format.publisher * *}        
      if$ add.year add.to.item
    }
    {
      format.address
      publisher empty$
        {"" *}
        {" " format.publisher * *}
      if$ add.year add.to.item
    }
  if$
}

FUNCTION {format.complementary.data}
{
  ""
  chapter empty$
  pages empty$
  and
    {format.totalpages add.to.item}
    'skip$
  if$
  "" add.volume add.number
  pages empty$
    {add.chapter}
    {add.pages}
  if$ add.to.item
}

%===============================================================================
% Formatacao das entradas
%===============================================================================
FUNCTION {article}
{
  ""
  format.author add.to.item
  #1 title format.title add.to.item
  journal emphasize
    add.address add.volume add.number add.pages add.month.year add.to.item
  format.note add.to.item

  write.item
  
}


FUNCTION {book}
{
  ""
  editor empty$
    {format.author}
    {format.editor}
  if$ add.to.item
  #1 title format.title add.to.item
  format.edition add.to.item
  format.publication add.to.item
  format.complementary.data add.to.item
  format.note add.to.item
  completion add.to.item

  write.item
}

FUNCTION {inbook}
{
  ""
  editor empty$
    {format.author}
    {format.editor}
  if$ add.to.item
  #1 title format.title add.to.item

  booktitle empty$ not
    {
      booktitle title = not
        {
          pop$ ""
          format.entrytitle add.to.item
          "In:"
            editor empty$
              {format.author}
              {format.editor}
            if$ add.to.item
            #1 booktitle format.title add.to.item
          add.to.item
        }
        'skip$
      if$
    }
    'skip$
  if$
  format.edition add.to.item
  format.publication add.to.item
  format.complementary.data add.to.item
  format.note add.to.item

  write.item
}

FUNCTION {incollection}
{
  ""
  format.author add.to.item
  #1 title format.title add.to.item
  "In:"
    editor empty$
      'skip$
      {format.editor add.to.item}
    if$ 
    #1 booktitle format.title add.to.item add.to.item
  format.edition add.to.item
  format.publication add.to.item
  format.complementary.data add.to.item
  format.note add.to.item

  write.item
}

FUNCTION {inproceedings}
{ ""
  format.author add.to.item
  #1 title format.title add.to.item
  "In:" booktitle emphasize
    eventedition empty$
      'skip$
      {eventedition add.period$ add.to.list}
    if$ 
  add.address add.to.item
  add.volume 
  add.number
  add.editor
  add.publisher 
  add.year add.to.item
  format.complementary.data add.to.item
  format.note add.to.item

  write.item
  
}



FUNCTION {manual}
{
  ""
  author empty$
    {format.entrytitle add.to.item}
    {
      format.author add.to.item
      #1 title format.title add.to.item
    }
  if$

  format.edition add.to.item
  format.publication add.to.item
  format.complementary.data add.to.item
  format.note add.to.item

  write.item
}

FUNCTION {mastersthesis}
{
  ""
  format.author add.to.item
  #1 title format.title add.to.item
  format.publication add.to.item
  str:mastersthesis format.type add.to.item
  format.note add.to.item

  write.item
}

FUNCTION {misc}
{
  ""
  author empty$
    {
      title empty$
        'skip$
        {format.entrytitle add.to.item}
      if$
    }
    {
      format.author add.to.item
      title empty$
        'skip$
        {#1 title format.title add.to.item}
      if$
    }
  if$
  format.edition add.to.item
  format.publication add.to.item
  format.complementary.data add.to.item
  format.note add.to.item

  write.item
}

% Este tipo de referencia nao e' padrao.
% Parametros obrigatorios: title, howpublished (ex. "WWW"), address
% (ex. "www.ufrgs.br/doc/texto.html"), year (quando a consulta foi feita)
% Parametros opcionais: author, month
FUNCTION {networkdoc}
{
  ""
  author empty$
    {format.entrytitle add.to.item}
    {
      format.author add.to.item
      #1 title format.title add.to.item
    }
  if$
  str:availableby howpublished str:at format.address * * *
  " (" format.month.year ")" * *
  * add.to.item
  format.note add.to.item

  write.item
}

FUNCTION {phdthesis}
{
  ""
  format.author add.to.item
  #1 title format.title add.to.item
  format.publication add.to.item
  str:phdthesis format.type add.to.item
  format.note add.to.item

  write.item
}

FUNCTION {proceedings}
{
  ""
  booktitle emphasize
  numedition empty$
    'skip$
    {eventedition add.period$ add.to.list}
  if$ add.year add.address add.to.item  
  add.publisher add.year add.to.item
  format.complementary.data add.to.item
  format.note add.to.item

  write.item
}

FUNCTION {techreport} {book}

FUNCTION {booklet} {misc}
FUNCTION {unpublished} {misc}
FUNCTION {default.type} {misc}

%===============================================================================
% Le as referencias feitas
%===============================================================================
READ

%===============================================================================
% Inicializa algumas variaveis globais
%===============================================================================
FUNCTION {init}
{
  "" 'last.label :=
  "" 'longest.label :=
  "a" 'current.letter :=
}

%===============================================================================
% Retorna na pilha o string a ser usado para a ordenacao das entradas.
%===============================================================================
FUNCTION {get.sortstring}
{
  author empty$ not
    {format.author}
    {
      editor empty$ not
        {format.editor}
        {
          title empty$ not
            {title}
            {
              key empty$ not
                {key}
                {"???"}
              if$
            }
          if$
        }
      if$
    }
  if$

  type$ "inbook" =
  booktitle empty$ not
  and
    {
      booktitle title = not
        {pop$ title}
        'skip$
      if$
    }
    'skip$
  if$

  type$ "proceedings" =
    {pop$ title}
    'skip$
  if$
}

%===============================================================================
% Define as ``keys'' para ordenacao
%===============================================================================
FUNCTION {set.sortkeys}
{
  get.sortstring purify$ "l" change.case$
  year empty$
    'skip$
    {year *}
  if$
  #1 entry.max$ substring$
  #1 global.max$ substring$
  'sort.key$ :=
}

%===============================================================================
% Retorna na pilha o string a ser usado para a construcao do label. Quase sempre
% sera' o sobrenome do primeiro autor, mas para alguns tipos de referencia pode
% ser o editor, a entidade ou o titulo
%===============================================================================
FUNCTION {get.labelstring}
{
  author empty$ not
    {author #1 "{ll}" format.name$}
    {
      editor empty$ not
        {editor #1 "{ll}" format.name$}
        {
          title empty$ not
            {title}
            {
              key empty$ not
                {key}
                {"???"}
              if$
            }
          if$
        }
      if$
    }
  if$

  type$ "proceedings" =
    {pop$ title}
    'skip$
  if$

  type$ "booklet" =
  type$ "manual" =
  or
  type$ "misc" =
  or
  type$ "proceedings" =
  or
  key empty$ not
  and
    {pop$ key}
    'skip$
  if$

  type$ "inbook" =
  booktitle empty$ not
  and
    {
      booktitle title = not
        {pop$ title}
        'skip$
      if$
    }
    'skip$
  if$
}

%===============================================================================
% Retorna na pilha o string a ser utilizado como label. Esta funcao recebe o
% string de onde o label deve ser extraido, e retorna os tres primeiros
% caracteres validos (ou seja, apostrofos ou hifens nao sao considerados).
% Esta funcao e' necessaria porque purify$ remove os acentos, e text.prefix$
% considera os caracteres invalidos. ``get.valid.label'' faz entao uma
% combinacao das duas.
%
% Parm:
%   o string de onde o label deve ser extraido 
%===============================================================================
FUNCTION {get.valid.label}
{
  's :=
  #3 'i :=
  s i text.prefix$
  {
    purify$ text.length$ #3 <
    s i #1 substring$ "" = not
    and
  }
    {
      #1 i + 'i :=
      s i text.prefix$
    }
  while$
  s i text.prefix$
}

%===============================================================================
% Constroi os labels, pegando as tres primeiras letras do nome do autor ou
% titulo (conforme retornado pela funcao ``get.labelstring''), mais os dois
% ultimos digitos do ano, se ele existir
%===============================================================================
FUNCTION {make.labels}
{
  get.labelstring get.valid.label "u" change.case$
  year empty$
    {"??"}
    {
      year purify$
      #-1 #2 substring$
    }
  if$ *
  'label :=
}

%===============================================================================
% Formata os labels, adicionando letras minusculas aos labels de mesmo autor e
% ano
%===============================================================================
FUNCTION {format.labels}
{
  last.label
  label 'last.label :=
  label =
    {
      label current.letter * 'label :=
      current.letter chr.to.int$
      #1 + int.to.chr$
      'current.letter :=
    }
    {"a" 'current.letter :=}
  if$
  longest.label purify$ width$ label purify$ width$ <
    {label 'longest.label :=}
    'skip$
  if$
}

%===============================================================================
% Inicia a definicao das referencias
%===============================================================================
FUNCTION {begin.bib}
{
  preamble$ empty$
    'skip$
    {preamble$ write$ newline$ newline$}
  if$
  "\begin{thebibliography}{" longest.label "}" * * write$ newline$
  newline$
}

%===============================================================================
% Termina a definicao das referencias
%===============================================================================
FUNCTION {end.bib}
{
  "\end{thebibliography}" write$ newline$
}

%===============================================================================
% main()
%===============================================================================
EXECUTE {init}
ITERATE {set.sortkeys}
SORT
ITERATE {make.labels}
ITERATE {format.labels}
EXECUTE {begin.bib}
ITERATE {call.type$}
EXECUTE {end.bib}
