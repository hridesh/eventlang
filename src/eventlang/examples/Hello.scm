(define seq (lambda (cmd1 cmd2) cmd2))

(define hello 
	(event (message))
)

(seq
	(when hello do (seq (print message) (print "World!")))
	(announce hello "Hello")
)