grammar EventLang;
import ForkLang; 
 
exp returns [Exp ast]: 
		va=varexp { $ast = $va.ast; }
		| num=numexp { $ast = $num.ast; }
		| str=strexp { $ast = $str.ast; }
		| bl=boolexp { $ast = $bl.ast; }
        | add=addexp { $ast = $add.ast; }
        | sub=subexp { $ast = $sub.ast; }
        | mul=multexp { $ast = $mul.ast; }
        | div=divexp { $ast = $div.ast; }
        | let=letexp { $ast = $let.ast; }
        | lam=lambdaexp { $ast = $lam.ast; }
        | call=callexp { $ast = $call.ast; }
        | i=ifexp { $ast = $i.ast; }
        | less=lessexp { $ast = $less.ast; }
        | eq=equalexp { $ast = $eq.ast; }
        | gt=greaterexp { $ast = $gt.ast; }
        | car=carexp { $ast = $car.ast; }
        | cdr=cdrexp { $ast = $cdr.ast; }
        | cons=consexp { $ast = $cons.ast; }
        | list=listexp { $ast = $list.ast; }
        | nl=nullexp { $ast = $nl.ast; }
        | lrec=letrecexp { $ast = $lrec.ast; }
        | ref=refexp { $ast = $ref.ast; }
        | deref=derefexp { $ast = $deref.ast; }
        | assign=assignexp { $ast = $assign.ast; }
        | free=freeexp { $ast = $free.ast; }
        | fork=forkexp { $ast = $fork.ast; }
        | lock=lockexp { $ast = $lock.ast; }
        | ulock=unlockexp { $ast = $ulock.ast; }
        | ev=eventexp { $ast = $ev.ast; }
        | an=announceexp { $ast = $an.ast; }
        | wh=whenexp { $ast = $wh.ast; }
        ;
  
eventexp returns [EventExp ast] 
        locals [ArrayList<String> formals = new ArrayList<String>()] :
        '(' Event
            '(' (id=Identifier { $formals.add($id.text); } )* ')'
        ')' { $ast = new EventExp($formals); }
        ;

announceexp returns [AnnounceExp ast] 
        locals [ArrayList<Exp> arguments = new ArrayList<Exp>()] :
        '(' Announce
			ev=exp
            '(' ( e=exp { $arguments.add($e.ast); } )* ')'
        ')' { $ast = new AnnounceExp($ev.ast,$arguments); }
        ;

whenexp returns [WhenExp ast] :
 		'(' When e1=exp
			Do e2=exp
		')' { $ast = new WhenExp($e1.ast, $e2.ast); }
		;	
 