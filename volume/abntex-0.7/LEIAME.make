$Header: /home/cvsroot/abntex/LEIAME.make,v 1.6 2002/12/04 12:39:24 gweber Exp $

O arquivo Makefile contém todas as funções relativas ao pacote
abnTeX bem como da homepage http://abntex.codigolivre.org.br


1) Documentação
  a) Gerar toda a documentação e coloca em compiled.docs:
	make doc

  b) Gerar partes da documentação (autoexplicativo):
	make doc-dvi
	make doc-ps
	make doc-pdf
	make doc-html

2) Homepage http://abntex.codigolivre.org.br
	make homepage

3) Limpeza
  a) de arquivos intermediários
	make doc-clean

  b) Remoção completa de compiled.doc
	make clean

4) Pacotes de distribuição
  a) arquivo *.tgz para Linux (tetex)
  	make linux-tgz
	make linux-doc
  b) arquivo *.zip para windows (árvore de diretórios igual ao do tetex
  	make windows-zip
	make windows-doc-zip

5) instalação linux/tetex
  provávelmente você terá de fazer isso como usuário root
  a) instalar
  	make install
  b) desinstalar
  	make uninstall
  c) instalação mínima a partir do cvs/abntex, isto permite instalar
     rápidamente os principais arquivos baixados via um cvs update
     Essa opção só é interessante para os desenvolvedores e quem quer
     testar a última versão ainda não disponível.
  	make install-from-cvs

6) geração de pacote RPM para distribuições linux com $TEXMFMAIN=/usr/share/texmf
  a) torne-se root
  b)
  	make conectiva-rpm
  c) os pacotes estarão em /usr/src/rpm/SPRMS e RPMS/noarch.
     obs: você deve fazer isso como root e tenha certeza de ter removido
     pacotes instalados de abntex e abntex-doc (rpm -e abntex abntex-doc)

7) nova versão/release:
  a) edite abntex_version, abntex_release, abntex.spec
  b) como root:
  	make new-release

8) enviar novos pacotes para upload.codigolivre.org.br
	make upload
   obs: não esqueça de gerar os pacotes primeiro

A SER FEITO:
make para instalação windows, se é que isso faz sentido.