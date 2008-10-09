/*
 * SafeOnline project.
 * 
 * Copyright 2006-2008 	Lin.k N.V. All rights reserved.
 * Lin.k N.V. proprietary/confidential. Use is subject to license terms.
 */

using System;

namespace safe_online_sdk_dotnet_test.test.cs
{
	public sealed class Constants
	{
		public static readonly string workDir = "C:\\Users\\devel\\Csharp";
		
		public static readonly string testPfxPath = workDir + "\\test.pfx";

		public static readonly string testPfxPassword = "secret";

		public static readonly string testCrtPath = workDir + "\\test.crt";
		
		public static readonly string olasCertPath = workDir + "\\olas.crt";
				
		public static readonly string olasHost = "sebeco-dev-11";
		
		public static readonly string wsLocation = olasHost + ":8443";
		
		public static readonly string localhost = "10.0.2.15";
		
		public static readonly string loginAttribute = "urn:net:lin-k:safe-online:attribute:login";
			
		private Constants()
		{
		}
	}
}
