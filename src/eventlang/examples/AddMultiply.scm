(define seq (lambda (cmd1 cmd2 cmd3) cmd3))

(define ev 
	(event (a b))
)

(seq
	(when ev do (print (+ a b)))
	(when ev do (print (* a b)))
	(announce ev 4 2)
)