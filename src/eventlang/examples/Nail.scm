(define nail (event (nailed)))
(define shoe (event (attached)))
(define horse (event (canrun)))
(define rider (event (rode)))
(define message (event (delivered)))
(define battle (event (won)))
(define kingdom (event (saved)))

(seq
	(when nail do 
		(if nailed 
			(seq (print "Nail is nailed") 
			     (announce shoe #t))
			(seq (print "Nail is missing...")
			     (announce shoe #f))
		)
	)
	
	(when shoe do 
		(if attached 
			(seq (print "Shoe is attached") 
			     (announce horse #t))
			(seq (print "For want of a nail the shoe was lost.")
			     (announce horse #f))
		)
	)
	
	(when horse do 
		(if canrun 
			(seq (print "Horse can run") 
			     (announce rider #t))
			(seq (print "For want of a shoe the horse was lost.")
			     (announce rider #f))
		)
	)
	
	(when rider do 
		(if rode 
			(seq (print "Rider rode.") 
			     (announce message #t))
			(seq (print "For want of a horse the rider was lost.")
			     (announce message #f))
		)
	)
	
	(when message do 
		(if delivered 
			(seq (print "Message was delivered.") 
			     (announce battle #t))
			(seq (print "For want of a rider the message was lost.")
			     (announce battle #f))
		)
	)
	
	(when battle do 
		(if won 
			(seq (print "Battle was won.") 
			     (announce kingdom #t))
			(seq (print "For want of a message the battle was lost.")
			     (announce kingdom #f))
		)
	)
	
	(when kingdom do 
		(if saved 
			(seq (print "Kingdom was saved.") 
			     (announce kingdom #t))
			(seq (print "For want of a battle the kingdom was lost.")
			     (print "And all for the want of a horseshoe nail."))
		)
	)
	
	(announce nail #f)
)
