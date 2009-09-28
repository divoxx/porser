#$Header: /home/cvsroot/abntex/Makefile,v 1.24.2.1 2003/05/30 12:22:24 gweber Exp $
#$Log: Makefile,v $
#Revision 1.24.2.1  2003/05/30 12:22:24  gweber
#Passa a fazer pdf.
#
#Revision 1.24  2003/03/31 21:10:50  gweber
#consertado um } que faltou
#
#Revision 1.23  2003/03/31 18:34:47  gweber
#css são copiadas em vez de linkadas
#
#

#altere a linha abaixo se o seu tetex não puder se detetado
#pelo comando kpsewich, na dúvida faça um "make test"
#TEXDIR= /tmp/test
TEXDIR= ${shell kpsewhich --expand-path='$$TEXMFMAIN'}

USRLOCAL=/usr/local/bin

#Do "man lyx":
#The system directory is determined by  searching  for  the
#       file  "chkconfig.ltx".
LYXDIR=${dir ${shell  find /usr/share/lyx -name chkconfig.ltx}}

#diretório corrente onde se expandiu o cvs do abntex
ABNTEXDIR=$(notdir $(shell pwd))

#diretório onde se encontra a documentação original
SOURCEDOC=$(shell pwd)/texmf/doc

#diretório para onde vai a documentação compilada
CDOCDIR=$(shell pwd)/compiled.docs

#script para gerar tabela de símbolos
GERATSS=$(shell pwd)/bin/geratss

#diretório onde se encontram os arquivos da homepage
HPDIR =pagina

#email da pessoa que envia os arquivos pro codigolivre
#edite se precisar
SUBMITTER=$(shell logname)@$(shell hostname)

#opções para comandos comuns:
OPS = --symbolic
OPSF= --symbolic --force
OPVR= --verbose --recursive
OPRF= --recursive --force
OPVF= --verbose --force
#opções comuns para tar
OPTAR= --owner=0 --group=0 -zchv

LATEXDOCDIR=${SOURCEDOC}/latex/abntex
BIBTEXDOCDIR=${SOURCEDOC}/bibtex/abntex

LATEXSDOCS=${wildcard ${LATEXDOCDIR}/abnt*.tex} ${wildcard ${LATEXDOCDIR}/tab*.tex}

LATEXSCSS=${wildcard ${LATEXDOCDIR}/*.css}
BIBTEXSDOCS=${wildcard ${BIBTEXDOCDIR}/*.tex}
BIBTEXSCSS=${wildcard ${BIBTEXDOCDIR}/*.css}

LATEXDOCS=${subst ${LATEXDOCDIR},${CDOCDIR},${LATEXSDOCS}}
LATEXCSS=${subst ${LATEXDOCDIR},${CDOCDIR},${LATEXSCSS}}

BIBTEXDOCS=${subst ${BIBTEXDOCDIR},${CDOCDIR},${BIBTEXSDOCS}}
BIBTEXCSS=${subst ${BIBTEXDOCDIR},${CDOCDIR},${BIBTEXSCSS}}

ALLSDOCS=${BIBTEXSDOCS} ${LATEXSDOCS}
ALLDOCS=${BIBTEXDOCS} ${LATEXDOCS}
ALLCSS=${BIBTEXCSS} ${LATEXCSS}

DVIDOCS=${ALLDOCS:.tex=.dvi}
PDFDOCS=${ALLDOCS:.tex=.pdf}
PSGZDOCS=${ALLDOCS:.tex=.ps.gz}
HTMLDOCS=${ALLDOCS:.tex=.html}

#variáveis de ambiente que precisam ser exportados para rodar a
#documentação corretamente
export TEXINPUTS=$(shell pwd)/texmf/tex/latex//:
export BIBINPUTS=$(shell pwd)/texmf/bibtex/bib//:
export BSTINPUTS=$(shell pwd)/texmf/bibtex/bst//:
export INDEXSTYLE=$(shell pwd)/texmf/tex/latex//:

homepage:
	cd ${HPDIR}; latex pagina; bibtex pagina;\
	cp pagina.bbl pagina.bbx; \
	sed "/bibitem/s/{ISO.*\|{NBR.*/[&]&/g" pagina.bbx > pagina.bbl;\
	latex pagina; latex pagina;\
	latex2html pagina
	
homepage-clean:
	cd ${HPDIR}; rm -rf *.aux *.blg  *.bbl  *.bbx *.dvi *.log *.pl;\
	rm -rf img*.png node*.html pagina.html index.html WARNINGS footnote.html


#aqui nos alteramos a variável TEX da regra implícita %.dvi:%.tex
#para as particularidades de cada documento
${CDOCDIR}/abnt-bibtex-doc.dvi: TEX=${LBLBLL}
${CDOCDIR}/abnt-bibtex-doc.pdf: TEX=${LBLBLL}
${CDOCDIR}/abnt-bibtex-alf-doc.dvi: TEX=${LBLL}
${CDOCDIR}/abnt-bibtex-alf-doc.pdf: TEX=${LBLL}
${CDOCDIR}/abnt-classe-doc.dvi: TEX=${LLL}
${CDOCDIR}/abnt-classe-doc.pdf: TEX=${LLL}
${CDOCDIR}/tabela-simbolos-doc.dvi: TEX=${LTBLL}
${CDOCDIR}/tabela-simbolos-doc.pdf: TEX=${LTBLL}
${CDOCDIR}/inpi-ex.dvi: TEX=${LLL}

#expande o nome do arquivo
FILE=	$(basename $(notdir $<))

#expande o comando de mudança de diretório
CDDIR=  cd $(dir $<);

#expande a versão deste pacote
VERSION= $(shell cat abntex_version)
RELEASE= $(shell cat abntex_release)

#arquivos dos pacotes
ABNTEX=abntex-${VERSION}-${RELEASE}
ABNTEXDOC=${subst abntex,abntex-doc,${ABNTEX}}
ABNTEXLYX=${subst abntex,abntex-lyx,${ABNTEX}}
ABNTEXTGZ=${ABNTEX}.tgz
ABNTEXDOCTGZ=${ABNTEXDOC}.tgz
ABNTEXZIP=${ABNTEX}.zip
ABNTEXDOCZIP=${ABNTEXDOC}.zip
ABNTEXSRCRPM=${ABNTEX}.src.rpm
ABNTEXRPM=${ABNTEX}.noarch.rpm
ABNTEXDOCRPM=${ABNTEXDOC}.noarch.rpm
ABNTEXLYXRPM=${ABNTEXLYX}.noarch.rpm

#diretórios padrão do rpm (Conectiva)
RPMS=/usr/src/rpm/RPMS/noarch
SRPMS=/usr/src/rpm/SRPMS
SOURCES=/usr/src/rpm/SOURCES

#diretório para geração de pacotes
PACKDIR=abntex-${VERSION}

LATEX=latex

#roda latex e bibtex de modo a resolver todas as referências
LBLBLL= cd ${CDOCDIR}; ${LATEX} ${FILE};\
	bibtex ${FILE}; ${LATEX} ${FILE}; \
	bibtex ${FILE}; ${LATEX} ${FILE}; latex ${FILE}

#documento com bibliografia simples
LBLL= cd ${CDOCDIR}; ${LATEX} ${FILE};\
	bibtex ${FILE}; ${LATEX} ${FILE}; ${LATEX} ${FILE}

#documento com bibliografia e lista de tabelas
LTBLL= cd ${CDOCDIR}; ${LATEX} ${FILE};\
	${GERATSS} ${FILE}; bibtex ${FILE};\
	${LATEX} ${FILE}; ${GERATSS} ${FILE}; ${LATEX} ${FILE}

#documento simples
LLL= cd ${CDOCDIR}; ${LATEX} ${FILE}; ${LATEX} ${FILE}; ${LATEX} ${FILE}

${CDOCDIR}:
	mkdir ${CDOCDIR}

${BIBTEXDOCS} ${BIBTEXCSS}: ${CDOCDIR}
	cp ${OPF} ${BIBTEXDOCDIR}/$(notdir $@) $@

${LATEXDOCS} ${LATEXCSS}: ${CDOCDIR}
	ln ${OPSF} ${LATEXDOCDIR}/$(notdir $@) $@

#dependência entre dvi<->ps
%.ps: %.dvi
	cd ${CDOCDIR}; \
	dvips -t a4 -o ${FILE}.ps ${FILE}

#compressão do arquivo ps
%.ps.gz: %.ps
	cd ${CDOCDIR}; gzip --force $<

#dependência entre pdf<->tex
%.pdf: %.tex
	$(subst latex,pdflatex,${TEX})$

HTMLOPT= -split 0 -noinfo -noaddress -html_version 4.0 -no_navigation

%.html: %.dvi
	${CDDIR} latex2html ${HTMLOPT} ${FILE}; \
	ln ${OPSF} ${FILE}/index.html ${FILE}.html

#cleanup remove arquivos intermediários desnecessários (exceto *.dvi)
#em compiled.docs
doc-clean: ${CDOCDIR}
	cd ${CDOCDIR};\
	rm ${OPVF} *.log *.blg *.bbl *.aux *.toc *.idx *.los *.lot *.out *.ilg\
	*.sigla* *.symbols*

#com clean somente os arquivos fonte sobram
clean:
	rm ${OPRF} ${CDOCDIR}

doc-remove-links:
	cd ${CDOCDIR};\
	rm ${OPVF} *.tex *.css


all:
	make doc

doc:
	make doc-clean
	make doc-dvi
	make doc-ps
	make doc-html
	make doc-clean
#enquanto tivermos problemas com pdf a linha seguinte segue comentada
	make doc-pdf
	make doc-clean
	make doc-remove-links

#cria links dos fontes da documentação para a criação de
#documentação compilada no diretório CDOCDIR
doc-links: ${ALLDOCS} ${ALLCSS}

doc-dvi: ${DVIDOCS}

doc-pdf: ${PDFDOCS}

doc-ps:	 ${PSGZDOCS}

doc-html: ${HTMLDOCS}

#O empacotamento para Linux

PACKLIST= ${PACKDIR}/Makefile \
	  ${PACKDIR}/abntex_version ${PACKDIR}/abntex_release \
	  ${PACKDIR}/*.lst \
	  ${PACKDIR}/LEIAME ${PACKDIR}/LEIAME.make ${PACKDIR}/LEIAME.linux \
	  ${PACKDIR}/LEIAME.administracao \
	  ${PACKDIR}/texmf ${PACKDIR}/bin ${PACKDIR}/lyx

DOCLIST = ${PACKDIR}/$(notdir ${CDOCDIR})

#principal pacote que contém os programas fontes do abnTeX
linux-tgz:
	cd ..; \
	ln ${OPSF} ${ABNTEXDIR} ${PACKDIR}; \
	rm ${OPVR} ${ABNTEXTGZ};\
	tar ${OPTAR}  --exclude-from ${ABNTEXDIR}/exc.lst -f ${ABNTEXTGZ} ${PACKLIST};\
	rm ${PACKDIR}

#pacote opcional com a documentação precompilada em dvi, pdf, ps e html
linux-doc-tgz:
	make doc
	cd ..; \
	ln ${OPSF} ${ABNTEXDIR} ${PACKDIR}; \
	rm ${OPVR} ${ABNTEXDOCTGZ};\
	tar ${OPTAR} -f ${ABNTEXDOCTGZ} ${DOCLIST};\
	rm ${PACKDIR}

#install e uninstall específico para tetex localizado em ${TEXDIR}
install:
	cp ${OPVR} --preserve texmf/* ${TEXDIR}
	cp ${OPVR} --preserve lyx/* ${LYXDIR}
	cp ${OPVR} --preserve bin/* ${USRLOCAL}
	texhash

#instalação mínima a partir de um diretório cvs/abntex útil para testes
#não inclui documentação
install-from-cvs:
	cp ${OPVR} --parents texmf/bibtex/*/abntex/*.b* $(TEXDIR)/..
	cp ${OPVR} --parents texmf/tex/latex/abntex/*.cls $(TEXDIR)/..
	cp ${OPVR} --parents texmf/tex/latex/abntex/*.sty $(TEXDIR)/..
	texhash

uninstall:
	rm ${OPRF} ${TEXDIR}/doc/latex/abntex;\
	rm ${OPRF} ${TEXDIR}/doc/bibtex/abntex
	rm ${OPRF} ${TEXDIR}/bibtex/bst/abntex;\
	rm ${OPRF} ${TEXDIR}/bibtex/bib/abntex;\
	rm ${OPRF} ${TEXDIR}/tex/latex/abntex;\
	rm ${OPRF} ${LYXDIR}abnt.layout;\
	rm ${OPRF} ${USRLOCAL}/geratss
	texhash

#para montar pacotes tipo rpm será melhor você ser root.
conectiva-rpm:
	if test "$(shell whoami)" = "root" ; then \
	cp ../${ABNTEXTGZ} ${SOURCES};\
	rpmbuild -ba --target=noarch --clean abntex.spec;\
	else\
	echo "you should be root to do this";\
	fi

windows-zip:
	cd ../; \
	rm ${OPVF} ${ABNTEXZIP};\
	ln ${OPSF} ${ABNTEXDIR} ${PACKDIR}; \
	zip -r ${ABNTEXZIP} ${PACKDIR} -x@${ABNTEXDIR}/zipexcl.lst;\
	rm ${PACKDIR}

windows-doc-zip:
	make doc
	cd ../; \
	rm ${OPVF} ${ABNTEXDOCZIP};\
	ln ${OPSF} ${ABNTEXDIR} ${PACKDIR}; \
	zip -r ${ABNTEXDOCZIP} ${PACKDIR}/compiled.docs;\
	rm ${PACKDIR}


new-release:
	make clean
	make doc
	make linux-tgz
	make linux-doc-tgz
	make windows-zip
	make windows-doc-zip
	make conectiva-rpm

upload:
	echo "open upload.codigolivre.org.br" > upload.ftp;\
	echo "user anonymous ${SUBMITTER}" >> upload.ftp;\
	echo "cd incoming" >> upload.ftp;\
	echo "passive" >> upload.ftp;\
	echo "put ../${ABNTEXTGZ} ${ABNTEXTGZ}" >> upload.ftp;\
	echo "put ../${ABNTEXDOCTGZ} ${ABNTEXDOCTGZ}" >> upload.ftp;\
	echo "put ../${ABNTEXZIP} ${ABNTEXZIP}" >> upload.ftp;\
	echo "put ../${ABNTEXDOCZIP} ${ABNTEXDOCZIP}" >> upload.ftp;\
	echo "put ${SRPMS}/${ABNTEXSRCRPM} ${ABNTEXSRCRPM}" >> upload.ftp;\
	echo "put ${RPMS}/${ABNTEXRPM} ${ABNTEXRPM}" >> upload.ftp;\
	echo "put ${RPMS}/${ABNTEXDOCRPM} ${ABNTEXDOCRPM}" >> upload.ftp;\
	echo "put ${RPMS}/${ABNTEXLYXRPM} ${ABNTEXLYXRPM}" >> upload.ftp;\
	echo "quit" >> upload.ftp
	ftp -v -n < upload.ftp
	rm upload.ftp

test:
	echo "diretório corrente:" ${ABNTEXDIR}
	echo "caminho para o tetex:" ${TEXDIR}
	echo "caminho para o LyX:" ${LYXDIR}
	echo "${LATEXDOCS}"
