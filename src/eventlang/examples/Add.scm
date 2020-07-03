(define ev 
	(event (a b))
)

(seq
	(when ev do (print (+ a b)))
	(announce ev 4 2)
)