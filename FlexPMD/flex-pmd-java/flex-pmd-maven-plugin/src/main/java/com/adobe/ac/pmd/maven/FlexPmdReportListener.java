/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.ac.pmd.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import net.sourceforge.pmd.IRuleViolation;
import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.ReportListener;
import net.sourceforge.pmd.stat.Metric;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.project.MavenProject;

/**
 * Writes the violations of a Flex PMD run through a Doxia {@link Sink}. It
 * replaces org.apache.maven.plugin.pmd.PmdReportListener of maven-pmd-plugin
 * 2.4, which is bound to the removed Doxia 1.0-alpha Sink API.
 */
class FlexPmdReportListener implements ReportListener
{
   private final boolean        aggregate;
   private final ResourceBundle bundle;
   private String               currentFilename;
   private int                  fileCount;
   private boolean              fileInitialized;
   private MavenProject         fileProject;
   private String               fileXrefLocation;
   private final Sink           sink;
   private final List< IRuleViolation > violations = new ArrayList< IRuleViolation >();

   FlexPmdReportListener( final Sink sinkToBeSet,
                          final ResourceBundle bundleToBeSet,
                          final boolean aggregateToBeSet )
   {
      sink = sinkToBeSet;
      bundle = bundleToBeSet;
      aggregate = aggregateToBeSet;
   }

   public void beginDocument()
   {
      sink.head();
      sink.title();
      sink.text( getTitle() );
      sink.title_();
      sink.head_();

      sink.body();

      sink.section( 1, null );
      sink.sectionTitle( 1, null );
      sink.text( getTitle() );
      sink.sectionTitle_( 1 );

      sink.paragraph();
      sink.text( bundle.getString( "report.pmd.pmdlink" )
            + " " );
      sink.link( "http://pmd.sourceforge.net/" );
      sink.text( "PMD" );
      sink.link_();
      sink.text( " " + PMD.VERSION + "." );
      sink.paragraph_();
      sink.section_( 1 );

      sink.section( 1, null );
      sink.sectionTitle( 1, null );
      sink.text( bundle.getString( "report.pmd.files" ) );
      sink.sectionTitle_( 1 );
   }

   public void beginFile( final File file,
                          final File sourceDirectory,
                          final MavenProject project,
                          final String xrefLocation )
   {
      fileProject = project;
      fileXrefLocation = xrefLocation;
      currentFilename = file.getAbsolutePath().substring( sourceDirectory.getAbsolutePath().length() + 1 ).replace( '\\',
                                                                                                                   '/' );
      fileInitialized = false;
   }

   public void endDocument()
   {
      if ( fileCount == 0 )
      {
         sink.text( bundle.getString( "report.pmd.noProblems" ) );
      }
      sink.section_( 1 );
      sink.body_();
      sink.flush();
      sink.close();
   }

   public void endFile( final File file )
   {
      if ( fileInitialized )
      {
         processViolations();
         sink.tableRows_();
         sink.table_();
         sink.section_( 2 );
      }
   }

   public void metricAdded( final Metric metric )
   {
      // metrics are not part of the report
   }

   public void ruleViolationAdded( final IRuleViolation violation )
   {
      if ( !fileInitialized )
      {
         sink.section( 2, null );
         sink.sectionTitle( 2, null );

         String title = currentFilename;

         if ( aggregate )
         {
            title = fileProject.getName()
                  + " - " + currentFilename;
         }
         sink.text( title );
         sink.sectionTitle_( 2 );

         sink.table();
         sink.tableRows( null, false );
         sink.tableRow();
         sink.tableHeaderCell();
         sink.text( bundle.getString( "report.pmd.column.violation" ) );
         sink.tableHeaderCell_();
         sink.tableHeaderCell();
         sink.text( bundle.getString( "report.pmd.column.line" ) );
         sink.tableHeaderCell_();
         sink.tableRow_();

         fileInitialized = true;
      }
      violations.add( violation );
   }

   private String getTitle()
   {
      return bundle.getString( "report.pmd.title" );
   }

   private void outputLineLink( final int line )
   {
      if ( fileXrefLocation != null )
      {
         sink.link( fileXrefLocation
               + "/" + currentFilename.replaceAll( "\\.java$",
                                                   ".html" ) + "#" + line );
      }
      sink.text( String.valueOf( line ) );
      if ( fileXrefLocation != null )
      {
         sink.link_();
      }
   }

   private void processViolations()
   {
      fileCount++;
      Collections.sort( violations,
                        new Comparator< IRuleViolation >()
                        {
                           public int compare( final IRuleViolation first,
                                               final IRuleViolation second )
                           {
                              return first.getBeginLine()
                                    - second.getBeginLine();
                           }
                        } );

      for ( final IRuleViolation violation : violations )
      {
         sink.tableRow();
         sink.tableCell();
         sink.text( violation.getDescription() );
         sink.tableCell_();

         sink.tableCell();
         final int beginLine = violation.getBeginLine();
         outputLineLink( beginLine );
         final int endLine = violation.getEndLine();
         if ( endLine != beginLine )
         {
            sink.text( " - " );
            outputLineLink( endLine );
         }
         sink.tableCell_();
         sink.tableRow_();
      }
      violations.clear();
   }
}
