all: latex
     latex: tc1.tex

	fig2dev -L eps ./Figuras/processos.fig ./Figuras/processos.eps
	fig2dev -L eps ./Figuras/appl.fig ./Figuras/appl.eps
	fig2dev -L eps ./Figuras/arvos.fig ./Figuras/arvos.eps
	fig2dev -L eps ./Figuras/CaseTime.fig ./Figuras/CaseTime.eps
	fig2dev -L eps ./Figuras/comunic.fig ./Figuras/comunic.eps
	fig2dev -L eps ./Figuras/execucao.fig ./Figuras/execucao.eps
	fig2dev -L eps ./Figuras/dependencia.fig ./Figuras/dependencia.eps
	fig2dev -L eps ./Figuras/esc.fig ./Figuras/esc.eps
	fig2dev -L eps ./Figuras/flowOSRTL.fig ./Figuras/flowOSRTL.eps
	fig2dev -L eps ./Figuras/flowprj.fig ./Figuras/flowprj.eps
	fig2dev -L eps ./Figuras/inter.fig ./Figuras/inter.eps
	fig2dev -L eps ./Figuras/niveis.fig ./Figuras/niveis.eps
	fig2dev -L eps ./Figuras/proc.fig ./Figuras/proc.eps
	fig2dev -L eps ./Figuras/total_flow.fig ./Figuras/total_flow.eps
	fig2dev -L eps ./Figuras/peqflx.fig ./Figuras/peqflx.eps
	fig2dev -L eps ./Figuras/OS_Main.fig ./Figuras/OS_Main.eps
	fig2dev -L eps ./Figuras/arvgui.fig ./Figuras/arvgui.eps
	fig2dev -L eps ./Figuras/comunicinter.fig ./Figuras/comunicinter.eps

	fig2dev -L eps ./Figuras/layersim.fig ./Figuras/layersim.eps
	fig2dev -L eps ./Figuras/compniv.fig ./Figuras/compniv.eps
	fig2dev -L eps ./Figuras/aplTLM.fig ./Figuras/aplTLM.eps
	fig2dev -L eps ./Figuras/aplTLMOS.fig ./Figuras/aplTLMOS.eps
	fig2dev -L eps ./Figuras/stackgener.fig ./Figuras/stackgener.eps
	fig2dev -L eps ./Figuras/stackesp.fig ./Figuras/stackesp.eps
	fig2dev -L eps ./Figuras/Apl_Main.fig ./Figuras/Apl_Main.eps
	fig2dev -L eps ./Figuras/OSTL_Main.fig ./Figuras/OSTL_Main.eps


	latex tc1.tex

	bibtex tc1

	latex tc1.tex
	latex tc1.tex

	dvips tc1.dvi -o Tc1.ps
	dvipdf tc1.dvi

	make clean

clean:
	rm *.dvi *.log *.aux *.toc *.bbl *.lot *.blg *.lof ./Figuras/*.eps


