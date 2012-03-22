/*
 * 02/25/2012
 *
 * Copyright (C) 2012 Robert Futrell
 * robert_futrell at users.sourceforge.net
 * http://fifesoft.com/rsyntaxtextarea
 *
 * This library is distributed under a modified BSD license.  See the included
 * RSTALanguageSupport.License.txt file for details.
 */
package org.fife.rsta.ac.js.ast;

import java.io.StringReader;
import java.util.HashMap;

import org.fife.rsta.ac.js.JavaScriptHelper;
import org.fife.rsta.ac.js.SourceCompletionProvider;
import org.fife.rsta.ac.js.completion.JSCompletion;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.ExpressionStatement;


public class VariableResolver {

	// HashMap of local variables mapped Name --> JSVariableDeclaration
	private HashMap localVariables = new HashMap();
	// HashMap of system variables mapped Name --> JSVariableDeclaration
	// system variables do not get cleared as they are always available to the
	// system
	private HashMap systemVariables = new HashMap();

	private SourceCompletionProvider provider;


	public VariableResolver(SourceCompletionProvider provider) {
		this.provider = provider;
	}


	public void addLocalVariable(JSVariableDeclaration declaration) {
		localVariables.put(declaration.getName(), declaration);
	}


	public void addSystemVariable(JSVariableDeclaration declaration) {
		systemVariables.put(declaration.getName(), declaration);
	}


	public void removeSystemVariable(String name) {
		systemVariables.remove(name);
	}


	/**
	 * Find JSVariableDeclaration and check is in scope of caret position
	 * 
	 * @param name
	 * @param dot
	 * @return JSVariableDeclaration from the name
	 */
	public JSVariableDeclaration findDeclaration(String name, int dot) {
		JSVariableDeclaration findDeclaration = findDeclaration(localVariables,
				name, dot);
		return findDeclaration == null ? findDeclaration(systemVariables, name,
				dot) : findDeclaration;
	}


	/**
	 * Find JSVariableDeclaration and check is in scope of caret position
	 * 
	 * @param name
	 * @param dot
	 * @return JSVariableDeclaration from the name
	 */
	public JSVariableDeclaration findDeclaration(HashMap variables,
			String name, int dot) {
		JSVariableDeclaration dec = (JSVariableDeclaration) variables.get(name);
		if (dec != null) {
			int decOffs = dec.getOffset();
			if (dot <= decOffs) {
				return dec;
			}
		}
		return null;
	}


	public TypeDeclaration getTypeDeclarationForVariable(String name, int dot) {
		JSVariableDeclaration dec = findDeclaration(name, dot);
		return dec != null ? dec.getTypeDeclaration() : null;
	}


	/**
	 * Clear all variables
	 */
	public void reset() {
		localVariables.clear();
	}


	/**
	 * Resolve the entered text by chopping up the text and working from left to
	 * right, resolving each type in turn
	 * 
	 * @param entered
	 * @param provider
	 * @param dot
	 * @return
	 */
	public TypeDeclaration resolveType(String entered,
			SourceCompletionProvider provider, int dot) {
		String[] enteredSplit = entered.split("\\.");
		TypeDeclaration variableType = getTypeDeclarationForVariable(
				enteredSplit[0], dot);
		if (variableType != null) {
			variableType = resolveTypeForFunction(variableType, enteredSplit,
					provider, dot);
		}
		else {
			// try to parse and resolve first element
			JSVariableDeclaration dec = parseEnteredContent(enteredSplit[0]);
			if (dec != null) {
				switch (dec.getTypeNode().getType()) {
					case Token.CALL:
					case Token.GETPROP:
						// get first part of String ... and work our way through
						// to the right
						variableType = provider.resolveTypeDeclation(dec
								.getTypeNode().toSource());
						break;

					default: // try to resolve the function type
					{
						// only resolve function type if split is > 0 other not
						// a function
						if (enteredSplit.length > 1) {
							variableType = resolveTypeForFunction(
									JSVariableDeclaration
											.tokenToTypeDeclaration(dec
													.getTypeNode(), provider),
									enteredSplit, provider, dot);
						}
						else {
							variableType = dec.getTypeDeclaration();
						}
					}
						break;

				}
			}
		}
		return variableType;
	}


	/**
	 * Resolve type for function (AstNode).toSource()
	 * 
	 * @param typeDec
	 * @param enteredSplit
	 * @param provider
	 * @param dot
	 * @return
	 */
	private TypeDeclaration resolveTypeForFunction(TypeDeclaration typeDec,
			String[] enteredSplit, SourceCompletionProvider provider, int dot) {

		TypeDeclaration resolved = null;
		if (provider.getJavaScriptTypesFactory() != null) {
			JavaScriptType cachedType = provider.getJavaScriptTypesFactory()
					.getCachedType(typeDec, provider.getJarManager(), provider);
			if (cachedType != null) {
				resolved = resolveType(cachedType, enteredSplit, 0, provider);
			}
		}

		return resolved;
	}


	/**
	 * Add all completions to CachedType
	 * 
	 * @param cachedType
	 * @param enteredText
	 * @param index
	 * @param provider
	 * @return
	 */
	private TypeDeclaration resolveType(JavaScriptType cachedType,
			String[] enteredText, int index, SourceCompletionProvider provider) {

		if (cachedType == null)
			return null;

		int newIndex = index + 1;
		if (newIndex == enteredText.length) {
			// reached the end of the road
			return cachedType.getType();
		}
		else {
			// get next string
			String text = enteredText[newIndex];
			text = JavaScriptHelper.convertTextToLookup(text);

			// look up JSCompletion
			JSCompletion completion = cachedType.getCompletion(text);
			if (completion != null) {
				String type = completion.getType();
				if (type != null) {
					TypeDeclaration newType = TypeDeclarationFactory.Instance()
							.getTypeDeclaration(type, true);
					if (newType != null) {
						if (provider.getJavaScriptTypesFactory() != null) {
							JavaScriptType newCachedType = provider
									.getJavaScriptTypesFactory().getCachedType(
											newType, provider.getJarManager(),
											provider);
							return resolveType(newCachedType, enteredText,
									newIndex, provider);
						}
					}
				}
			}
		}
		return null;
	}


	/**
	 * Initially parse the content to check for new String literal, new Object
	 * etc..
	 * 
	 * @param text
	 * @return
	 */
	private JSVariableDeclaration parseEnteredContent(String text) {
		CompilerEnvirons env = new CompilerEnvirons();
		env.setIdeMode(true);
		Parser parser = new Parser(env);
		StringReader r = new StringReader(text);
		try {
			AstRoot root = parser.parse(r, null, 0);
			JSVariableDeclaration dec = new JSVariableDeclaration("root", 0,
					provider);

			dec.setTypeNode(((ExpressionStatement) root.getFirstChild())
					.getExpression());
			return dec;

		} catch (Exception e) {
		}
		return null;
	}

}