(define eveven (event (n)))
(define evodd (event (n)))

(define even
	(lambda (n)
		(if (= n 0) #t
			(if (odd (- n 1)) #t
				#f
			)
		)
	)
)

(define odd 
	(lambda (n)
		(if (= n 0) #f
			(if (even (- n 1)) #t
				#f
			)
		)
	)
)

(define choose 
	(lambda (n)
		(if (even n) eveven 
			evodd
		)
	)
)
			
(seq
	(when eveven do (print "Even:" n))
	(when evodd do (print "Odd:" n))
	(announce (choose 4) 4)
)