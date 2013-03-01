package org.json4s.macroimpls.macrohelpers

import scala.reflect.macros.Context
import org.json4s.{JValue, JNothing, JObject}


class MacroHelpers[C <: Context](val c1: C) {
  
  import c1.universe._
  
  def LIT[B](x: B) = c1.Expr[B](Literal(Constant(x)))
  def CONCAT(a: c1.Expr[String], b: c1.Expr[String]) = reify{a.splice + b.splice}
  
  // For building objects that take type parameters
  def typeArgumentTree(t: c1.Type): c1.Tree = t match {
    case TypeRef(_, _, typeArgs @ _ :: _) => AppliedTypeTree(
          Ident(t.typeSymbol), typeArgs map (t => typeArgumentTree(t)) )
    case _                                => Ident(t.typeSymbol.name)
  }
    
  def getVars(tpe: Type): List[Symbol] = tpe.members.filter(_.asTerm.isVar).toList
  def getVals(tpe: Type): List[Symbol] = tpe.members.filter(_.asTerm.isVal).toList
  
  def getNonConstructorVars(tpe: Type): List[Symbol] = {
    // Make sure that the param isn't part of the constructor
    val ctorParams = tpe.member(nme.CONSTRUCTOR).asMethod.paramss.flatten.map(_.name.toTermName.toString.trim)
    for{
      sym <- getVars(tpe) if !ctorParams.exists(sym.name.toTermName.toString.trim == _)
    } yield sym
  }


  // For extracting a JValue from a JObject
  def getObjectField(name: c1.Expr[String], params: c1.Expr[JValue]):(TermName, c1.Expr[JValue], Tree) = {
    import PrimativeHelpers.getFieldByName

    val freshNme = newTermName(c1.fresh("freshJValue$"))
    val freshObj = c1.Expr[JValue](Ident(freshNme))

    val objFieldTree = ValDef( // Need to use a fresh name to avoid stuff like 'val nme = nme'
      Modifiers(),
      freshNme,
      TypeTree(typeOf[JValue]),
      reify(
        getFieldByName(params.splice, name.splice)
          .getOrElse(JNothing)
      ).tree
    )
    (freshNme, freshObj, objFieldTree)
  }
  
}
