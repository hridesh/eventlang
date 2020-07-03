(define hello 
	(event (message))
)

(seq
	(when hello do (seq (print message) (print "World!")))
	(announce hello "Hello")
)